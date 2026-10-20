package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrammarImporterTest {
    private val catalog = TestFixture.catalog

    private fun imported(text: String): Grammar = GrammarImporter(catalog).parse("A.1 Test\n$text")

    @Test fun bundledGrammarKeepsAnnexSectionsAndRuleText() {
        val grammar = TestFixture.grammar
        assertEquals(750, grammar.sources.size)
        assertTrue(grammar.sources.getValue("module_declaration").section.startsWith("A."))
        assertTrue(grammar.sources.getValue("binary_value").text.contains("binary_digit"))
    }

    @Test fun sourceRootsReachModuleAndNumericGrammar() {
        val reachable = TestFixture.grammar.reachableFrom("source_text", "library_text")
        assertTrue("module_declaration" in reachable)
        assertTrue("hex_value" !in reachable) // Lexer alias replaces the character rule.
        assertTrue("@lexer_hex_value" in reachable)
        assertTrue(reachable.none { it.startsWith("?") })
    }

    @Test fun documentedPdfHeadingErrorsAreDiagnosedAndRecovered() {
        val grammar = TestFixture.grammar
        assertTrue(grammar.diagnostics.any { "incomplete_class_scoped_type" in it })
        assertTrue(grammar.diagnostics.any { "class_qualifier" in it })
        assertTrue(grammar.byLeft["incomplete_class_scoped_type"].orEmpty().isNotEmpty())
        assertTrue(grammar.byLeft["class_qualifier"].orEmpty().isNotEmpty())
    }

    @Test fun literalBracketsAreTokensRatherThanOptionalGroups() {
        val grammar = imported("source_text ::= ⟦[⟧ identifier ⟦]⟧")
        val explorer = Explorer(grammar, catalog, "source_text")
        assertEquals(setOf("OpenBracket"), explorer.nextNames())
        explorer.accept("OpenBracket", "Identifier", "CloseBracket")
        assertTrue(explorer.isComplete())
    }

    @Test fun optionalGroupAllowsBothBranches() {
        val grammar = imported("source_text ::= ⟦module⟧ [ ⟦automatic⟧ ] identifier ⟦endmodule⟧")
        val explorer = Explorer(grammar, catalog, "source_text")
        explorer.accept("ModuleKeyword")
        assertEquals(setOf("AutomaticKeyword", "Identifier"), explorer.nextNames())
        explorer.accept("Identifier", "EndModuleKeyword")
        assertTrue(explorer.isComplete())
        explorer.reset("source_text")
        explorer.accept("ModuleKeyword", "AutomaticKeyword", "Identifier", "EndModuleKeyword")
        assertTrue(explorer.isComplete())
    }

    @Test fun alternativesPreserveTheChosenBranch() {
        val flat = imported("source_text ::= ⟦module⟧ ⟦automatic⟧ | ⟦module⟧ ⟦static⟧")
        val explorer = Explorer(flat, catalog, "source_text")
        explorer.accept("ModuleKeyword")
        assertEquals(setOf("AutomaticKeyword", "StaticKeyword"), explorer.nextNames())
        explorer.accept("StaticKeyword")
        assertTrue(explorer.isComplete())
        assertFalse(explorer.advance("AutomaticKeyword"))
    }

    @Test fun repetitionCanBeEmptyOrRepeated() {
        val grammar = imported("source_text ::= ⟦module⟧ { ⟦;⟧ } ⟦endmodule⟧")
        val explorer = Explorer(grammar, catalog, "source_text")
        explorer.accept("ModuleKeyword", "EndModuleKeyword")
        assertTrue(explorer.isComplete())
        explorer.reset("source_text")
        explorer.accept("ModuleKeyword", "Semicolon", "Semicolon", "Semicolon", "EndModuleKeyword")
        assertTrue(explorer.isComplete())
    }

    @Test fun unresolvedNameIsReportedAndDoesNotCreateGraphEdge() {
        val grammar = imported("source_text ::= ⟦module⟧ unknown_annex_symbol")
        assertTrue("?unknown_annex_symbol" in grammar.unresolvedSymbols)
        assertTrue("?unknown_annex_symbol" in grammar.reachableFrom("source_text"))
        assertTrue(TokenGraph(grammar, catalog).successors("ModuleKeyword").isEmpty())
    }

    @Test fun duplicateRuleIsDiagnosedWithoutOverwritingFirstSource() {
        val grammar = imported("source_text ::= ⟦module⟧\nsource_text ::= ⟦endmodule⟧")
        assertTrue(grammar.diagnostics.any { it.contains("Duplicate rule source_text") })
        assertEquals("source_text ::= ⟦module⟧", grammar.sources.getValue("source_text").text)
    }

    @Test fun sourceProvenanceSurvivesExpansion() {
        val grammar = imported("source_text ::= ⟦module⟧ [ ⟦automatic⟧ ] identifier")
        assertTrue(grammar.productions.all { it.source == "source_text" })
        assertTrue(grammar.productions.any { it.left.startsWith("@source_text_") })
    }
}
