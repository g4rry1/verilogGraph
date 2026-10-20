package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExplorerTest {
    @Test fun moduleDeclarationAllowsItsAnnexHeaderAlternatives() {
        assertEquals(setOf("OpenParenthesis", "ExternKeyword", "MacromoduleKeyword", "ModuleKeyword"),
            TestFixture.explorer().nextNames())
    }

    @Test fun moduleKeywordOffersLifetimeOrName() {
        val explorer = TestFixture.explorer().accept("ModuleKeyword")
        assertEquals(setOf("AutomaticKeyword", "StaticKeyword", "Identifier"), explorer.nextNames())
    }

    @Test fun moduleNameOffersTheFourHeaderContinuations() {
        val explorer = TestFixture.explorer().accept("ModuleKeyword", "Identifier")
        assertEquals(setOf("Hash", "OpenParenthesis", "Semicolon", "ImportKeyword"), explorer.nextNames())
    }

    @Test fun minimalModuleCanComplete() {
        val explorer = TestFixture.explorer().accept(
            "ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword"
        )
        assertTrue(explorer.isComplete())
    }

    @Test fun automaticAndStaticAreIndependentAlternatives() {
        for (lifetime in listOf("AutomaticKeyword", "StaticKeyword")) {
            val explorer = TestFixture.explorer().accept(
                "ModuleKeyword", lifetime, "Identifier", "Semicolon", "EndModuleKeyword"
            )
            assertTrue(explorer.isComplete())
        }
    }

    @Test fun emptyPortListCanComplete() {
        val explorer = TestFixture.explorer().accept(
            "ModuleKeyword", "Identifier", "OpenParenthesis", "CloseParenthesis",
            "Semicolon", "EndModuleKeyword"
        )
        assertTrue(explorer.isComplete())
    }

    @Test fun unfinishedHeaderIsNotComplete() {
        val explorer = TestFixture.explorer().accept("ModuleKeyword", "Identifier")
        assertFalse(explorer.isComplete())
        assertFalse(explorer.advance("EndModuleKeyword"))
        assertEquals(listOf("ModuleKeyword", "Identifier"), explorer.prefix.map { it.name })
    }

    @Test fun unknownTokenDoesNotModifyPrefix() {
        val explorer = TestFixture.explorer().accept("ModuleKeyword")
        assertFalse(explorer.advance("NotATokenKind"))
        assertEquals(listOf("ModuleKeyword"), explorer.prefix.map { it.name })
    }

    @Test fun backRestoresEarlierAlternatives() {
        val explorer = TestFixture.explorer().accept("ModuleKeyword", "Identifier")
        assertTrue(explorer.back())
        assertEquals(listOf("ModuleKeyword"), explorer.prefix.map { it.name })
        assertEquals(setOf("AutomaticKeyword", "StaticKeyword", "Identifier"), explorer.nextNames())
        assertTrue(explorer.back())
        assertFalse(explorer.back())
        assertEquals(setOf("OpenParenthesis", "ExternKeyword", "MacromoduleKeyword", "ModuleKeyword"),
            explorer.nextNames())
    }

    @Test fun resetChangesContextAndClearsPrefix() {
        val explorer = TestFixture.explorer().accept("ModuleKeyword", "Identifier")
        explorer.reset("assignment_pattern")
        assertEquals("assignment_pattern", explorer.root)
        assertTrue(explorer.prefix.isEmpty())
        assertEquals(setOf("ApostropheOpenBrace"), explorer.nextNames())
    }

    @Test fun nonexistentRuleIsRejected() {
        val explorer = TestFixture.explorer()
        assertFailsWith<IllegalArgumentException> { explorer.reset("no_such_rule") }
    }

    @Test fun assignmentPatternStartsWithOneCompositeToken() {
        val explorer = TestFixture.explorer("assignment_pattern")
        explorer.accept("ApostropheOpenBrace")
        assertTrue(explorer.nextNames().isNotEmpty())
        assertFalse(explorer.advance("Apostrophe"))
    }

    @Test fun libraryDeclarationUsesSynthesizedFileNameKind() {
        val explorer = TestFixture.explorer("library_declaration")
        explorer.accept("LibraryKeyword", "Identifier")
        assertTrue("IncludeFileName" in explorer.nextNames())
    }

    @Test fun systemTimingCheckStartsWithSystemIdentifier() {
        assertEquals(setOf("SystemIdentifier"), TestFixture.explorer("system_timing_check").nextNames())
    }
}
