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
        assertContains(explorer.candidate("PackageKeyword").starts, "package_declaration")
    }

    @Test fun colonAfterEndmoduleContinuesTheSameModule() {
        val explorer = file().accept("ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword")
        val colon = explorer.candidate("Colon")
        assertEquals(emptySet(), colon.starts)
        assertContains(colon.rules, "module_declaration")
        explorer.accept("Colon", "Identifier")
        assertTrue(explorer.isComplete())
        assertContains(explorer.candidate("ModuleKeyword").starts, "module_declaration")
    }

    @Test fun topLevelEndmoduleCannotBeFollowedByAlwaysOrAnotherEndmodule() {
        val explorer = file().accept("ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword")
        assertFalse(explorer.next().any { it.token.name == "AlwaysKeyword" })
        assertFalse(explorer.next().any { it.token.name == "EndModuleKeyword" })
        assertTrue(explorer.back())
        assertFalse(explorer.next().any { it.token.name == "PackageKeyword" })
        assertContains(explorer.candidate("AlwaysKeyword").starts, "always_construct")
    }

    @Test fun nestedEndmoduleCanStartAnAlwaysConstructInOuterModule() {
        val explorer = file().accept(
            "ModuleKeyword", "Identifier", "Semicolon",
            "ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword"
        )
        assertFalse(explorer.isComplete())
        assertContains(explorer.candidate("AlwaysKeyword").starts, "always_construct")
        assertEquals(emptySet(), explorer.candidate("EndModuleKeyword").starts)
        assertContains(explorer.candidate("ModuleKeyword").starts, "module_declaration")
        explorer.accept("EndModuleKeyword")
        assertTrue(explorer.isComplete())
    }

    @Test fun isolatedModuleRuleDoesNotOfferNextTopLevelConstruct() {
        val explorer = TestFixture.explorer().accept(
            "ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword"
        )
        assertEquals(setOf("Colon"), explorer.next().mapTo(linkedSetOf()) { it.token.name })
        assertEquals(emptySet(), explorer.candidate("Colon").starts)
    }

    @Test fun cliShowsBoundaryOnlyForContextualTransitions() {
        val output = StringWriter()
        runCli(TestFixture.catalog, TestFixture.grammar,
            StringReader("step module\nstep Identifier\nstep ;\nstep endmodule\nnext module\nnext :\nglobal endmodule module\nquit\n"),
            output, "source_text")
        val text = output.toString()
        assertTrue(Regex("ModuleKeyword \\(module\\).*\\[начало: [^]]*module_declaration").containsMatchIn(text))
        assertTrue(Regex("Colon \\(:\\).*\\[без начала нового правила\\]").containsMatchIn(text))
        val global = text.substringAfter("Глобальные продолжения EndModuleKeyword")
            .substringBefore("vg>")
        assertFalse(global.contains("[начало:"))
        assertFalse(global.contains("[без начала нового правила]"))
    }
}
