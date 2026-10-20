package dev.veriloggraph

import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransitionContextTest {
    private fun file(): Explorer = TestFixture.explorer().apply { reset("source_text") }

    private fun Explorer.candidate(kind: String): Candidate =
        next().single { it.token.name == kind }

    @Test fun nextModuleAfterEndmoduleStartsAnotherDescription() {
        val explorer = file().accept("ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword")
        assertTrue(explorer.isComplete())
        val module = explorer.candidate("ModuleKeyword")
        assertContains(module.starts, "description")
        assertContains(module.starts, "module_declaration")
        assertTrue(module.beginsConstruction)
        assertEquals("module_declaration", module.displayedConstruction())
        val packageToken = explorer.candidate("PackageKeyword")
        assertContains(packageToken.starts, "package_declaration")
        assertTrue(packageToken.beginsConstruction)
    }

    @Test fun colonAfterEndmoduleContinuesTheSameModule() {
        val explorer = file().accept("ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword")
        val colon = explorer.candidate("Colon")
        assertEquals(emptySet(), colon.starts)
        assertFalse(colon.beginsConstruction)
        assertContains(colon.rules, "module_declaration")
        explorer.accept("Colon")
        val label = explorer.candidate("Identifier")
        assertContains(label.starts, "module_identifier")
        assertFalse(label.beginsConstruction, "An end label stays inside the previous module")
        explorer.accept("Identifier")
        assertTrue(explorer.isComplete())
        assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
    }

    @Test fun topLevelEndmoduleCannotBeFollowedByAlwaysOrAnotherEndmodule() {
        val explorer = file().accept("ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword")
        assertFalse(explorer.next().any { it.token.name == "AlwaysKeyword" })
        assertFalse(explorer.next().any { it.token.name == "EndModuleKeyword" })
        assertTrue(explorer.back())
        assertFalse(explorer.next().any { it.token.name == "PackageKeyword" })
        assertContains(explorer.candidate("AlwaysKeyword").starts, "always_construct")
        assertTrue(explorer.candidate("AlwaysKeyword").beginsConstruction)
    }

    @Test fun nestedEndmoduleCanStartAnAlwaysConstructInOuterModule() {
        val explorer = file().accept(
            "ModuleKeyword", "Identifier", "Semicolon",
            "ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword"
        )
        assertFalse(explorer.isComplete())
        assertContains(explorer.candidate("AlwaysKeyword").starts, "always_construct")
        assertTrue(explorer.candidate("AlwaysKeyword").beginsConstruction)
        assertEquals(emptySet(), explorer.candidate("EndModuleKeyword").starts)
        assertFalse(explorer.candidate("EndModuleKeyword").beginsConstruction)
        assertContains(explorer.candidate("ModuleKeyword").starts, "module_declaration")
        assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
        explorer.accept("EndModuleKeyword")
        assertTrue(explorer.isComplete())
    }

    @Test fun isolatedModuleRuleDoesNotOfferNextTopLevelConstruct() {
        val explorer = TestFixture.explorer().accept(
            "ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword"
        )
        assertEquals(setOf("Colon"), explorer.next().mapTo(linkedSetOf()) { it.token.name })
        assertEquals(emptySet(), explorer.candidate("Colon").starts)
        assertFalse(explorer.candidate("Colon").beginsConstruction)
    }

    @Test fun identifierInModuleHeaderStartsASubruleButNotANewConstruction() {
        val explorer = file().accept("ModuleKeyword")
        val identifier = explorer.candidate("Identifier")
        assertContains(identifier.starts, "module_identifier")
        assertFalse(identifier.beginsConstruction)
    }

    @Test fun aRepeatedSiblingDiffersFromAnOptionalSuffixInASmallGrammar() {
        val grammar = GrammarImporter(TestFixture.catalog).parse(
            "A.1 Test\nsource_text ::= { description }\n" +
                "description ::= ⟦module⟧ [ ⟦:⟧ module_identifier ]\n" +
                "module_identifier ::= identifier"
        )
        val explorer = Explorer(grammar, TestFixture.catalog, "source_text")
        assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
        explorer.accept("ModuleKeyword")
        assertFalse(explorer.candidate("Colon").beginsConstruction)
        explorer.accept("Colon")
        assertContains(explorer.candidate("Identifier").starts, "module_identifier")
        assertFalse(explorer.candidate("Identifier").beginsConstruction)
        explorer.accept("Identifier")
        assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
    }

    @Test fun cliShowsBoundaryOnlyForContextualTransitions() {
        val output = StringWriter()
        runCli(TestFixture.catalog, TestFixture.grammar,
            StringReader("step module\nstep Identifier\nstep ;\nstep endmodule\nnext module\nnext :\nstep :\nnext Identifier\nglobal endmodule module\nquit\n"),
            output, "source_text")
        val text = output.toString()
        assertTrue(Regex("ModuleKeyword \\(module\\).*\\[новая конструкция: module_declaration\\]").containsMatchIn(text))
        assertTrue(Regex("Colon \\(:\\).*\\[продолжение\\]").containsMatchIn(text))
        assertTrue(Regex("Identifier.*\\[продолжение; подправило: module_identifier\\]").containsMatchIn(text))
        val global = text.substringAfter("Глобальные продолжения EndModuleKeyword")
            .substringBefore("vg>")
        assertFalse(global.contains("[новая конструкция:"))
        assertFalse(global.contains("[продолжение"))
    }
}
