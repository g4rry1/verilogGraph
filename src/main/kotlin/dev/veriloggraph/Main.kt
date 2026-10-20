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
        if (args.first() == "--audit") println(grammar.unresolvedSymbols.joinToString())
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
        println("Grammar path checks passed")
        return
    }

    SwingUtilities.invokeLater { GraphWindow(catalog, grammar).isVisible = true }
}
