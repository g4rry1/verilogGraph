package dev.veriloggraph

import java.util.ArrayDeque

data class Candidate(val token: TokenKind, val rules: Set<String>)

/** Earley prefix recognizer. Each click extends one grammar-valid token prefix. */
class Explorer(private val grammar: Grammar, private val catalog: TokenCatalog) {
    private data class Item(val production: Int, val dot: Int, val origin: Int)

    private val chart = mutableListOf(linkedSetOf<Item>())
    private val productive: Set<String> = findProductive()
    val prefix = mutableListOf<TokenKind>()
    var root: String = "module_declaration"
        private set

    init {
        reset(root)
    }

    fun reset(rule: String = "module_declaration") {
        require(rule in grammar.byLeft) { "Unknown grammar rule: $rule" }
        root = rule
        prefix.clear()
        chart.clear()
        chart.add(linkedSetOf())
        for (production in grammar.byLeft.getValue(root)) {
            if (production.isProductive()) chart[0] += Item(production.id, 0, 0)
        }
        close(0)
    }

    fun next(): List<Candidate> {
        val groups = linkedMapOf<String, MutableSet<String>>()
        for (item in chart.last()) {
            val production = grammar.productions[item.production]
            val token = production.right.getOrNull(item.dot) as? Symbol.Token ?: continue
            groups.getOrPut(token.kind) { linkedSetOf() } += production.source
        }
        return groups.mapNotNull { (name, rules) ->
            catalog.byName[name]?.let { Candidate(it, rules) }
        }.sortedWith(compareBy({ it.token.spelling == null }, { it.token.label }))
    }

    fun advance(name: String): Boolean {
        val token = catalog.byName[name] ?: return false
        if (next().none { it.token.name == name }) return false
        val current = chart.last()
        val following = linkedSetOf<Item>()
        for (item in current) {
            val production = grammar.productions[item.production]
            if (production.right.getOrNull(item.dot) == Symbol.Token(name)) {
                following += item.copy(dot = item.dot + 1)
            }
        }
        chart += following
        prefix += token
        close(chart.lastIndex)
        return true
    }

    fun back(): Boolean {
        if (prefix.isEmpty()) return false
        prefix.removeAt(prefix.lastIndex)
        chart.removeAt(chart.lastIndex)
        return true
    }

    fun isComplete(): Boolean = chart.last().any { item ->
        val production = grammar.productions[item.production]
        production.left == root && item.origin == 0 && item.dot == production.right.size
    }

    private fun close(index: Int) {
        val queue = ArrayDeque(chart[index])
        while (queue.isNotEmpty()) {
            val item = queue.removeFirst()
            val production = grammar.productions[item.production]
            val next = production.right.getOrNull(item.dot)
            if (next is Symbol.Rule) {
                if (next.name !in productive) continue
                for (child in grammar.byLeft[next.name].orEmpty()) {
                    if (child.isProductive()) add(index, Item(child.id, 0, index), queue)
                }
                for (completed in chart[index].toList()) {
                    val child = grammar.productions[completed.production]
                    if (child.left == next.name && completed.origin == index && completed.dot == child.right.size) {
                        add(index, item.copy(dot = item.dot + 1), queue)
                    }
                }
            } else if (next == null) {
                for (parent in chart[item.origin].toList()) {
                    val parentProduction = grammar.productions[parent.production]
                    if (parentProduction.right.getOrNull(parent.dot) == Symbol.Rule(production.left)) {
                        add(index, parent.copy(dot = parent.dot + 1), queue)
                    }
                }
            }
        }
    }

    private fun add(index: Int, item: Item, queue: ArrayDeque<Item>) {
        if (chart[index].add(item)) queue += item
    }

    private fun Production.isProductive(): Boolean = right.all { symbol ->
        symbol is Symbol.Token || symbol is Symbol.Rule && symbol.name in productive
    }

    private fun findProductive(): Set<String> {
        val known = mutableSetOf<String>()
        var changed: Boolean
        do {
            changed = false
            for (production in grammar.productions) {
                if (production.right.all { symbol ->
                        symbol is Symbol.Token || symbol is Symbol.Rule && symbol.name in known
                    }) {
                    if (known.add(production.left)) changed = true
                }
            }
        } while (changed)
        return known
    }
}
