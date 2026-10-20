package dev.veriloggraph

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A separate executable witness for every imported rule that has a token-level expansion. */
class RuleCoverageTest {
    private val grammar = TestFixture.grammar
    private val catalog = TestFixture.catalog
    private val characterOnly = setOf(
        "z_or_x", "time_unit", "exp", "quoted_string_item", "triple_quoted_string_item",
        "string_escape_seq", "one_line_comment", "white_space"
    )

    private val roots: Map<String, String> = grammar.sources.keys.mapNotNull { name ->
        val root = when {
            name in grammar.byLeft -> name
            "@lexer_$name" in grammar.byLeft -> "@lexer_$name"
            else -> null
        }
        root?.let { name to it }
    }.toMap()

    /** Fixed point over productions; this is independent of Explorer's Earley chart. */
    private val witnesses: Map<String, List<String>> by lazy { shortestWitnesses() }

    private fun shortestWitnesses(): Map<String, List<String>> {
        val witnesses = mutableMapOf<String, List<String>>()
        var changed: Boolean
        do {
            changed = false
            for (production in grammar.productions) {
                val sequence = mutableListOf<String>()
                var ready = true
                for (symbol in production.right) {
                    when (symbol) {
                        is Symbol.Token -> sequence += symbol.kind
                        is Symbol.Rule -> {
                            val child = witnesses[symbol.name]
                            if (child == null) { ready = false; break }
                            sequence += child
                        }
                    }
                }
                if (!ready) continue
                val old = witnesses[production.left]
                if (old == null || sequence.size < old.size) {
                    witnesses[production.left] = sequence
                    changed = true
                }
            }
        } while (changed)
        return witnesses
    }

    @Test fun everyReachableNamedConstructionIsCovered() {
        val reachable = grammar.reachableFrom("source_text", "library_text")
            .filterTo(mutableSetOf()) { it in grammar.sources }
        val missing = reachable - roots.keys
        assertTrue(missing.isEmpty(), "Reachable rules without token-level expansion: $missing")
        assertEquals(703, reachable.size)
    }

    @Test fun characterAndTriviaRulesAreTheOnlyUnproductiveExpandedNames() {
        val unproductive = roots.filterValues { it !in witnesses }.keys
        assertEquals(characterOnly, unproductive)
        val reachable = grammar.reachableFrom("source_text", "library_text")
        assertTrue(unproductive.none { it in reachable })
    }

    @TestFactory fun eachCharacterOnlyRuleIsRecordedOutsideTheTokenGraph(): List<DynamicTest> {
        val reachable = grammar.reachableFrom("source_text", "library_text")
        return characterOnly.sorted().map { name ->
            DynamicTest.dynamicTest(name) {
                val root = roots[name]
                assertTrue(root != null, "$name is no longer an expanded Annex A rule")
                assertTrue(root !in witnesses, "$name unexpectedly gained a token-level derivation")
                assertTrue(name !in reachable, "$name became reachable from source_text/library_text")
            }
        }
    }

    @TestFactory fun eachExpandedAnnexRuleHasACompletableTokenPath(): List<DynamicTest> {
        val explorer = Explorer(grammar, catalog)
        return roots.filterValues { it in witnesses }.map { (name, root) ->
            DynamicTest.dynamicTest(name) {
                val path = requireNotNull(witnesses[root])
                explorer.reset(root)
                for ((index, kind) in path.withIndex()) {
                    assertTrue(explorer.advance(kind),
                        "$name rejects $kind at step $index in ${path.joinToString()}")
                }
                assertTrue(explorer.isComplete(),
                    "$name cannot end after ${path.joinToString()}")
            }
        }
    }

    @TestFactory fun eachCollapsedLexicalRuleMapsToOneSlangKind(): List<DynamicTest> {
        return (grammar.sources.keys - roots.keys).map { name ->
            DynamicTest.dynamicTest(name) {
                val wrapper = GrammarImporter(catalog).parse("A.1 Test\nsource_text ::= $name")
                val right = wrapper.byLeft.getValue("source_text").single().right
                assertEquals(1, right.size, "$name should be a single token kind")
                val token = right.single() as? Symbol.Token
                assertTrue(token != null && token.kind in catalog.byName,
                    "$name is not mapped to a slang TokenKind: $right")
                val explorer = Explorer(wrapper, catalog, "source_text")
                assertTrue(explorer.advance(token.kind))
                assertTrue(explorer.isComplete())
            }
        }
    }
}
