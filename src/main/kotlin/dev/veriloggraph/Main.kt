package dev.veriloggraph

import javax.swing.SwingUtilities
import java.nio.file.Path
import java.nio.file.Files

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--preview") System.setProperty("java.awt.headless", "true")
    val catalog = TokenCatalog.bundled()
    val grammar = Grammar.bundled(catalog)
    val explorer = Explorer(grammar, catalog)
    val graph = TokenGraph(grammar, catalog)

    if (args.firstOrNull() == "--export-dot") {
        require(args.size == 2) { "Usage: --export-dot output.dot" }
        Files.writeString(Path.of(args[1]), graph.toDot())
        println("Wrote ${graph.vertexCount} vertices and ${graph.edgeCount} edges to ${args[1]}")
        return
    }

    if (args.firstOrNull() == "--preview") {
        require(args.size == 2) { "Usage: --preview output.png" }
        saveModulePreview(catalog, grammar, Path.of(args[1]))
        println("Wrote module graph preview to ${args[1]}")
        return
    }

    if (args.firstOrNull() == "--check" || args.firstOrNull() == "--audit") {
        println("slang TokenKind vertices: ${catalog.kinds.size}")
        println("Annex A rules: ${grammar.sources.size}; productions: ${grammar.productions.size}")
        println("Derived graph: ${graph.vertexCount} vertices; ${graph.edgeCount} edges")
        println("Unresolved grammar symbols: ${grammar.unresolvedSymbols.size}")
        val reachable = grammar.reachableFrom("source_text", "library_text")
        println("Reachable from source_text/library_text: ${reachable.count { it in grammar.sources }} named rules; " +
            "${reachable.count { it.startsWith("?") }} unresolved symbols")
        if (args.first() == "--audit") {
            println("Reachable unresolved: " + reachable.filter { it.startsWith("?") }.sorted().joinToString())
            println("All unresolved: " + grammar.unresolvedSymbols.joinToString())
        }
        println("Import diagnostics: ${grammar.diagnostics.size}")
        grammar.diagnostics.take(12).forEach(::println)
        val module = requireNotNull(catalog.bySpelling["module"])
        check(explorer.advance(module.name)) { "module is not a valid module_declaration prefix" }
        val next = explorer.next()
        println("After module: ${next.joinToString { it.token.label }}")
        println("Global edges from module: ${graph.successors(module.name).joinToString { it.token.label }}")
        check(next.map { it.token.name }.toSet() ==
            setOf("Identifier", "AutomaticKeyword", "StaticKeyword"))
        check(graph.successors(module.name).map { it.token.name }.toSet() ==
            setOf("Identifier", "AutomaticKeyword", "StaticKeyword"))
        check(explorer.advance("Identifier"))
        println("After module Identifier: ${explorer.next().joinToString { it.token.label }}")
        check(explorer.next().map { it.token.label }.toSet() == setOf("#", "(", ";", "import"))
        check(explorer.advance("Semicolon"))
        check(explorer.next().any { it.token.name == "EndModuleKeyword" })
        check(explorer.advance("EndModuleKeyword"))
        check(explorer.isComplete())
        explorer.reset()
        check(explorer.advance("ModuleKeyword"))
        check(explorer.advance("AutomaticKeyword"))
        check(explorer.advance("Identifier"))
        check(explorer.advance("Semicolon"))
        check(explorer.advance("EndModuleKeyword"))
        check(explorer.isComplete())
        explorer.reset()
        for (kind in listOf("ModuleKeyword", "Identifier", "OpenParenthesis",
                "CloseParenthesis", "Semicolon", "EndModuleKeyword")) {
            check(explorer.advance(kind)) { "Invalid module header token: $kind" }
        }
        check(explorer.isComplete())
        explorer.reset("assignment_pattern")
        check(explorer.next().map { it.token.name }.toSet() == setOf("ApostropheOpenBrace"))
        check(explorer.advance("ApostropheOpenBrace"))
        check("OpenBrace" in graph.successors("Identifier").map { it.token.name })
        check("CloseBracket" in graph.successors("Identifier").map { it.token.name })
        explorer.reset("binary_number")
        println("Binary number starts: ${explorer.next().joinToString { it.token.name }}")
        check(explorer.next().map { it.token.name }.toSet() == setOf("IntegerLiteral", "IntegerBase"))
        check(explorer.advance("IntegerBase"))
        check(explorer.next().map { it.token.name }.toSet() ==
            setOf("IntegerLiteral", "Identifier", "Question"))
        explorer.reset("system_timing_check")
        check(explorer.next().map { it.token.name }.toSet() == setOf("SystemIdentifier"))
        explorer.reset("library_declaration")
        check(explorer.advance("LibraryKeyword"))
        check(explorer.advance("Identifier"))
        check(explorer.advance("IncludeFileName"))
        check("IncDirKeyword" in graph.successors("Minus").map { it.token.name })
        explorer.reset("init_val")
        check(explorer.advance("IntegerLiteral"))
        check("IntegerBase" in explorer.next().map { it.token.name })
        check(explorer.advance("IntegerBase"))
        check(explorer.next().map { it.token.name }.toSet() == setOf("IntegerLiteral", "Identifier"))
        explorer.reset("combinational_entry")
        check(explorer.next().map { it.token.name }.toSet() ==
            setOf("IntegerLiteral", "Identifier", "Question"))
        check(explorer.advance("Identifier"))
        check("Colon" in explorer.next().map { it.token.name })
        explorer.reset("pulse_control_specparam")
        check("Identifier" in explorer.next().map { it.token.name })
        check(grammar.byLeft["constraint_expression"].orEmpty().any { production ->
            Symbol.Token("MinusArrow") in production.right
        })
        check(grammar.byLeft["trans_range_list"].orEmpty().any { production ->
            Symbol.Token("OpenBracket") in production.right && Symbol.Token("MinusArrow") in production.right
        })
        explorer.reset("number")
        check(explorer.advance("IntegerBase"))
        check(explorer.next().map { it.token.name }.toSet() ==
            setOf("IntegerLiteral", "Identifier", "Question", "RealLiteral"))
        check(explorer.advance("Question"))
        check("Identifier" in explorer.next().map { it.token.name })
        check("RealLiteral" in explorer.next().map { it.token.name })
        fun checkNumericPath(root: String, vararg kinds: String) {
            explorer.reset(root)
            for (kind in kinds) {
                check(explorer.advance(kind)) { "$root rejects ${kinds.joinToString()} at $kind" }
            }
            check(explorer.isComplete()) { "$root cannot end after ${kinds.joinToString()}" }
        }
        // Annex A values 1x?0, 7z?3, and 1e2a?f are split by slang's lexer.
        checkNumericPath("binary_number", "IntegerBase", "IntegerLiteral", "Identifier",
            "Question", "IntegerLiteral")
        checkNumericPath("octal_number", "IntegerBase", "IntegerLiteral", "Identifier",
            "Question", "IntegerLiteral")
        checkNumericPath("hex_number", "IntegerBase", "RealLiteral", "Identifier",
            "Question", "Identifier")
        checkNumericPath("hex_number", "IntegerLiteral", "IntegerBase", "Question",
            "RealLiteral", "Question", "IntegerLiteral")
        explorer.reset("@lexer_binary_value")
        check(explorer.advance("Identifier"))
        check(explorer.next().map { it.token.name }.toSet() == setOf("Question"))
        explorer.reset("@lexer_hex_value")
        check(explorer.advance("RealLiteral"))
        check(explorer.next().map { it.token.name }.toSet() == setOf("Identifier", "Question"))
        check("binary_value" in graph.evidence("IntegerLiteral", "Identifier"))
        check("octal_value" in graph.evidence("Identifier", "Question"))
        check("hex_value" in graph.evidence("RealLiteral", "Identifier"))
        explorer.reset("edge_control_specifier")
        check(explorer.advance("EdgeKeyword"))
        check(explorer.advance("OpenBracket"))
        check(explorer.next().map { it.token.name }.toSet() == setOf("IntegerLiteral", "Identifier"))
        check(grammar.byLeft["class_qualifier"].orEmpty().isNotEmpty())
        check(grammar.byLeft["incomplete_class_scoped_type"].orEmpty().isNotEmpty())
        println("Grammar path checks passed")
        return
    }

    SwingUtilities.invokeLater { GraphWindow(catalog, grammar).isVisible = true }
}
