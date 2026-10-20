package dev.veriloggraph

import java.nio.charset.StandardCharsets

sealed interface Symbol {
    data class Token(val kind: String) : Symbol
    data class Rule(val name: String) : Symbol
}

data class Production(val id: Int, val left: String, val right: List<Symbol>, val source: String)
data class RuleSource(val name: String, val section: String, val text: String)

class Grammar internal constructor(
    val productions: List<Production>,
    val sources: Map<String, RuleSource>,
    val diagnostics: List<String>,
) {
    val byLeft = productions.groupBy { it.left }
    val unresolvedSymbols: Set<String> = productions.asSequence()
        .flatMap { it.right.asSequence() }
        .filterIsInstance<Symbol.Rule>()
        .map { it.name }
        .filter { it.startsWith("?") }
        .toSortedSet()

    fun reachableFrom(vararg roots: String): Set<String> {
        val reached = linkedSetOf<String>()
        val queue = ArrayDeque(roots.toList())
        while (queue.isNotEmpty()) {
            val name = queue.removeFirst()
            if (!reached.add(name)) continue
            for (production in byLeft[name].orEmpty()) for (symbol in production.right) {
                if (symbol is Symbol.Rule && symbol.name !in reached) queue.add(symbol.name)
            }
        }
        return reached
    }

    companion object {
        fun bundled(catalog: TokenCatalog): Grammar {
            val text = requireNotNull(Grammar::class.java.getResourceAsStream("/ieee1800-2023-annex-a.txt"))
                .bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            return GrammarImporter(catalog).parse(text)
        }
    }
}

private sealed interface Expr {
    data class Word(val value: String) : Expr
    data class Sequence(val parts: List<Expr>) : Expr
    data class Choice(val alternatives: List<Expr>) : Expr
    data class Optional(val child: Expr) : Expr
    data class Repeat(val child: Expr) : Expr
}

private class ExpressionParser(private val words: List<String>) {
    private var position = 0

    fun parse(): Expr {
        val result = choice(null)
        require(position == words.size) { "Unexpected '${words[position]}'" }
        return result
    }

    private fun choice(end: String?): Expr {
        val alternatives = mutableListOf<Expr>()
        while (true) {
            alternatives += sequence(end)
            if (position >= words.size || words[position] != "|") break
            position++
        }
        return if (alternatives.size == 1) alternatives.first() else Expr.Choice(alternatives)
    }

    private fun sequence(end: String?): Expr {
        val items = mutableListOf<Expr>()
        while (position < words.size) {
            val word = words[position]
            if (word == end || word == "|") break
            position++
            items += when (word) {
                "[" -> Expr.Optional(group("]"))
                "{" -> Expr.Repeat(group("}"))
                "]", "}" -> error("Unexpected '$word'")
                else -> Expr.Word(word)
            }
        }
        return Expr.Sequence(items)
    }

    private fun group(end: String): Expr {
        val inner = choice(end)
        require(position < words.size && words[position] == end) { "Missing '$end'" }
        position++
        return inner
    }
}

class GrammarImporter(private val catalog: TokenCatalog) {
    private val problems = mutableListOf<String>()
    private val output = mutableListOf<Production>()
    private val sourceMap = linkedMapOf<String, RuleSource>()
    private var hidden = 0

    fun parse(text: String): Grammar {
        val rawRules = splitRules(text)
        val names = rawRules.keys
        for ((name, source) in rawRules) {
            // Annex A describes lexemes as characters; slang already groups them into TokenKind.
            if (lexicalClass(name) != null) continue
            try {
                val words = tokenize(source.text.substringAfter("::="))
                val expression = ExpressionParser(words).parse()
                for (sequence in alternatives(expression)) emit(name, sequence, name, names)
            } catch (exception: IllegalArgumentException) {
                problems += "$name (${source.section}): ${exception.message}"
            } catch (exception: IllegalStateException) {
                problems += "$name (${source.section}): ${exception.message}"
            }
        }
        return Grammar(output, sourceMap, problems)
    }

    private fun splitRules(text: String): Map<String, RuleSource> {
        val entries = linkedMapOf<String, RuleSource>()
        val start = Regex("^([A-Za-z_][A-Za-z_0-9]*(?:,[0-9]+)?)\\s*::=\\s*(.*)$")
        val malformed = Regex("^[A-Za-z_][A-Za-z_0-9]*\\s*(?:::\\s+=|:=).*$")
        var section = "Annex A"
        var ruleSection = section
        var name: String? = null
        val body = StringBuilder()
        val numericName = Regex("^(.*?)([0-9]+)$")

        fun finish() {
            val current = name ?: return
            val source = RuleSource(current, ruleSection, body.toString().trim())
            if (current in entries) problems += "Duplicate rule $current"
            else entries[current] = source
            body.clear()
        }

        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.matches(Regex("^A\\.\\d.*"))) {
                finish()
                name = null
                section = trimmed
                continue
            }
            if (malformed.matches(trimmed)) {
                finish()
                name = null
                problems += "Malformed rule heading in $section: $trimmed"
                continue
            }
            val match = start.matchEntire(trimmed)
            if (match != null) {
                finish()
                val rawName = match.groupValues[1]
                val footnote = numericName.matchEntire(rawName)
                name = if (rawName.contains(',')) rawName.replace(Regex("[0-9]+(?:,[0-9]+)+$"), "")
                else if (footnote != null && Regex("\\b${Regex.escape(rawName)}\\b")
                        .findAll(text).count() == 1) footnote.groupValues[1] else rawName
                ruleSection = section
                body.append(name).append(" ::= ").append(match.groupValues[2])
            } else if (name != null && trimmed.isNotEmpty()) {
                body.append(' ').append(trimmed)
            }
        }
        finish()
        sourceMap.putAll(entries)
        return entries
    }

    private fun tokenize(text: String): List<String> {
        // The plain-text transcription loses red/bold terminal markup in Annex A.
        // We leave ambiguous rules in diagnostics instead of creating an edge.
        val normalized = text
            .replace(Regex("(?<=[}\\]])[0-9]+(?:,[0-9]+)*"), "")
            .replace(Regex("(?<=[;,)])[0-9]+(?:,[0-9]+)*"), "")
            .replace("| | |", "| ⟨pipe⟩ |")
            .replace("#(", "# (")
            .replace("*::*", "* :: *")
            .replace("(*", "( *")
            .replace("*)", "* )")
        return Regex("\\s+|[\\[\\]{}]|[^\\s\\[\\]{}]+")
            .findAll(normalized)
            .map { it.value }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun alternatives(expr: Expr): List<Expr> =
        if (expr is Expr.Choice) expr.alternatives else listOf(expr)

    private fun emit(left: String, expr: Expr, source: String, names: Set<String>) {
        val parts = if (expr is Expr.Sequence) expr.parts else listOf(expr)
        val right = parts.map { part -> symbolFor(part, source, names) }
        output += Production(output.size, left, right, source)
    }

    private fun symbolFor(expr: Expr, source: String, names: Set<String>): Symbol = when (expr) {
        is Expr.Word -> resolve(expr.value, names)
        is Expr.Sequence, is Expr.Choice, is Expr.Optional, is Expr.Repeat -> {
            val inner = "@${source}_${hidden++}"
            when (expr) {
                is Expr.Optional -> {
                    output += Production(output.size, inner, emptyList(), source)
                    alternatives(expr.child).forEach { emit(inner, it, source, names) }
                }
                is Expr.Repeat -> {
                    output += Production(output.size, inner, emptyList(), source)
                    for (alternative in alternatives(expr.child)) {
                        val sequence = if (alternative is Expr.Sequence) alternative.parts else listOf(alternative)
                        val right = sequence.map { symbolFor(it, source, names) } + Symbol.Rule(inner)
                        output += Production(output.size, inner, right, source)
                    }
                }
                else -> alternatives(expr).forEach { emit(inner, it, source, names) }
            }
            Symbol.Rule(inner)
        }
    }

    private fun resolve(word: String, names: Set<String>): Symbol {
        if (word == "⟨pipe⟩") return Symbol.Token(requireNotNull(catalog.bySpelling["|"]).name)
        val plain = word.replace(Regex("[0-9]+$"), "")
        catalog.bySpelling[word]?.let { return Symbol.Token(it.name) }
        if (word.startsWith('$') && word.length > 1 && word[1].isLetter())
            return Symbol.Token("SystemIdentifier")
        if (word.matches(Regex("[0-9]+"))) return Symbol.Token("IntegerLiteral")
        if (word.startsWith('"') && word.endsWith('"')) return Symbol.Token("StringLiteral")
        if (word in setOf("option", "type_option", "sample", "std", "randomize", "incdir"))
            return Symbol.Token("Identifier")
        lexicalClass(word)?.let { return Symbol.Token(it) }
        lexicalClass(plain)?.let { return Symbol.Token(it) }
        if (word in names) return Symbol.Rule(word)
        if (plain in names) return Symbol.Rule(plain)
        catalog.bySpelling[plain]?.let { return Symbol.Token(it.name) }
        return Symbol.Rule("?${word}")
    }

    private fun lexicalClass(name: String): String? = when (name) {
        "identifier", "simple_identifier", "escaped_identifier" -> "Identifier"
        "system_tf_identifier" -> "SystemIdentifier"
        "unsigned_number" -> "IntegerLiteral"
        "real_number", "fixed_point_number" -> "RealLiteral"
        "time_literal" -> "TimeLiteral"
        "string_literal", "quoted_string", "triple_quoted_string" -> "StringLiteral"
        "unbased_unsized_literal" -> "UnbasedUnsizedLiteral"
        else -> null
    }
}
