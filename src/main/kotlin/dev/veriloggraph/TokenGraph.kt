package dev.veriloggraph

/** The union of immediate token adjacencies licensed by productive Annex A rules. */
class TokenGraph(grammar: Grammar, private val catalog: TokenCatalog) {
    private data class Facts(
        val first: MutableSet<String> = linkedSetOf(),
        val last: MutableSet<String> = linkedSetOf(),
        var nullable: Boolean = false,
        var productive: Boolean = false,
    )

    private val facts = mutableMapOf<String, Facts>()
    private val edges = mutableMapOf<String, MutableMap<String, MutableSet<String>>>()
    private val rootRules = grammar.reachableFrom("source_text", "library_text")
    private val rootProductions = grammar.productions.filter { it.left in rootRules }

    val vertexCount: Int get() = catalog.kinds.size
    val edgeCount: Int get() = edges.values.sumOf { it.size }

    init {
        for (production in rootProductions) facts.getOrPut(production.left, ::Facts)
        var changed: Boolean
        do {
            changed = false
            for (production in rootProductions) {
                val fact = facts.getValue(production.left)
                val productive = production.right.all { symbol ->
                    symbol is Symbol.Token || symbol is Symbol.Rule && facts[symbol.name]?.productive == true
                }
                if (!productive) continue
                if (!fact.productive) { fact.productive = true; changed = true }
                if (production.right.all(::nullable) && !fact.nullable) {
                    fact.nullable = true; changed = true
                }
                for (symbol in production.right) {
                    if (fact.first.addAll(first(symbol))) changed = true
                    if (!nullable(symbol)) break
                }
                for (symbol in production.right.asReversed()) {
                    if (fact.last.addAll(last(symbol))) changed = true
                    if (!nullable(symbol)) break
                }
            }
        } while (changed)

        for (production in rootProductions) {
            if (production.right.any { it is Symbol.Rule && facts[it.name]?.productive != true }) continue
            val symbols = production.right
            for (i in symbols.indices) {
                for (j in i + 1 until symbols.size) {
                    if ((i + 1 until j).any { !nullable(symbols[it]) }) break
                    for (from in last(symbols[i])) for (to in first(symbols[j])) {
                        edges.getOrPut(from) { linkedMapOf() }
                            .getOrPut(to) { linkedSetOf() } += production.source
                    }
                }
            }
        }
    }

    fun successors(name: String): List<Candidate> = edges[name].orEmpty().mapNotNull { (to, sources) ->
        catalog.byName[to]?.let { Candidate(it, sources) }
    }.sortedWith(compareBy({ it.token.spelling == null }, { it.token.label }))

    fun evidence(from: String, to: String): Set<String> = edges[from]?.get(to).orEmpty()

    fun toDot(): String = buildString {
        append("digraph SystemVerilogTokens {\n")
        append("  graph [label=\"IEEE 1800-2023 Annex A / slang TokenKind\", labelloc=t];\n")
        append("  node [shape=box, style=rounded];\n")
        for (token in catalog.kinds) {
            append("  ").append(token.name).append(" [label=\"")
                .append(escape(token.label)).append("\\n")
                .append(escape(token.name)).append("\"];\n")
        }
        for ((from, targets) in edges.toSortedMap()) for ((to, rules) in targets.toSortedMap()) {
            append("  ").append(from).append(" -> ").append(to)
                .append(" [tooltip=\"").append(escape(rules.joinToString()))
                .append("\"];\n")
        }
        append("}\n")
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun nullable(symbol: Symbol): Boolean =
        symbol is Symbol.Rule && facts[symbol.name]?.nullable == true

    private fun first(symbol: Symbol): Set<String> = when (symbol) {
        is Symbol.Token -> setOf(symbol.kind)
        is Symbol.Rule -> facts[symbol.name]?.first.orEmpty()
    }

    private fun last(symbol: Symbol): Set<String> = when (symbol) {
        is Symbol.Token -> setOf(symbol.kind)
        is Symbol.Rule -> facts[symbol.name]?.last.orEmpty()
    }
}
