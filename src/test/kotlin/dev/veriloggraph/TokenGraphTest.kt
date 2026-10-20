package dev.veriloggraph

import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenGraphTest {
    private val catalog = TestFixture.catalog
    private val grammar = TestFixture.grammar
    private val graph = TestFixture.graph

    @Test fun everyGraphVertexComesFromSlangCatalog() {
        assertEquals(catalog.kinds.size, graph.vertexCount)
        assertTrue(graph.edgeCount > 10_000)
        for (token in catalog.kinds) {
            assertTrue(graph.successors(token.name).all { it.token.name in catalog.byName })
        }
    }

    @Test fun globalModuleSuccessorsMatchDocumentedHeader() {
        assertEquals(setOf("AutomaticKeyword", "StaticKeyword", "Identifier"),
            graph.successors("ModuleKeyword").map { it.token.name }.toSet())
    }

    @Test fun everyShownEdgeHasARealAnnexRuleAsEvidence() {
        for (from in listOf("ModuleKeyword", "Identifier", "IntegerBase", "RealLiteral")) {
            for (candidate in graph.successors(from)) {
                assertTrue(candidate.rules.isNotEmpty(), "$from -> ${candidate.token.name}")
                assertTrue(candidate.rules.all { it in grammar.sources }, "$from -> ${candidate.token.name}")
                assertEquals(candidate.rules, graph.evidence(from, candidate.token.name))
            }
        }
    }

    @Test fun numericEdgesRetainTheValueRuleAsProvenance() {
        assertTrue("binary_value" in graph.evidence("IntegerLiteral", "Identifier"))
        assertTrue("octal_value" in graph.evidence("Identifier", "Question"))
        assertTrue("hex_value" in graph.evidence("RealLiteral", "Identifier"))
    }

    @Test fun nullableRuleCreatesBothImmediateSuccessors() {
        val small = GrammarImporter(catalog).parse(
            "A.1 Test\nsource_text ::= ⟦module⟧ [ ⟦automatic⟧ ] identifier"
        )
        val local = TokenGraph(small, catalog)
        assertEquals(setOf("AutomaticKeyword", "Identifier"),
            local.successors("ModuleKeyword").map { it.token.name }.toSet())
        assertEquals(setOf("Identifier"),
            local.successors("AutomaticKeyword").map { it.token.name }.toSet())
    }

    @Test fun unreachableRuleDoesNotCreateGlobalEdge() {
        val small = GrammarImporter(catalog).parse(
            "A.1 Test\nsource_text ::= ⟦module⟧ ⟦endmodule⟧\n" +
                "unused ::= ⟦automatic⟧ ⟦static⟧"
        )
        val local = TokenGraph(small, catalog)
        assertEquals(setOf("EndModuleKeyword"), local.successors("ModuleKeyword").map { it.token.name }.toSet())
        assertTrue(local.successors("AutomaticKeyword").isEmpty())
        assertTrue(local.evidence("AutomaticKeyword", "StaticKeyword").isEmpty())
    }

    @Test fun unresolvedRuleCannotLicenseAnEdge() {
        val small = GrammarImporter(catalog).parse(
            "A.1 Test\nsource_text ::= ⟦module⟧ missing_rule ⟦endmodule⟧"
        )
        val local = TokenGraph(small, catalog)
        assertTrue(local.successors("ModuleKeyword").isEmpty())
    }

    @Test fun dotExportContainsAllVerticesAndEvidence() {
        val dot = graph.toDot()
        assertTrue(dot.startsWith("digraph SystemVerilogTokens {"))
        assertTrue("ModuleKeyword [label=\"module\\nModuleKeyword\"]" in dot)
        assertTrue("ModuleKeyword -> Identifier" in dot)
        assertTrue("RealLiteral -> Identifier" in dot)
        assertTrue("tooltip=\"hex_value\"" in dot || "hex_value" in dot)
        assertFalse("?unknown_annex_symbol" in dot)
    }

    @Test fun previewIsAReadablePngAtExpectedSize() {
        val path = Files.createTempFile("veriloggraph-preview-", ".png")
        try {
            saveModulePreview(catalog, grammar, path)
            val image = ImageIO.read(path.toFile())
            assertEquals(900, image.width)
            assertEquals(450, image.height)
            assertTrue(Files.size(path) > 1_000)
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
