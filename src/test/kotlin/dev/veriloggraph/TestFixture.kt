package dev.veriloggraph

internal object TestFixture {
    val catalog: TokenCatalog = TokenCatalog.bundled()
    val grammar: Grammar = Grammar.bundled(catalog)
    val graph: TokenGraph = TokenGraph(grammar, catalog)

    fun explorer(root: String = "module_declaration") = Explorer(grammar, catalog).apply { reset(root) }
}

internal fun Explorer.nextNames(): Set<String> = next().mapTo(linkedSetOf()) { it.token.name }

internal fun Explorer.accept(vararg names: String): Explorer {
    for (name in names) {
        check(advance(name)) { "$root rejected $name after ${prefix.joinToString { it.name }}" }
    }
    return this
}
