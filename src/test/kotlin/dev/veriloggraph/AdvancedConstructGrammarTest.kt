package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Additional paths from Annex A sections A.1–A.8, including UDP and specify forms. */
class AdvancedConstructGrammarTest {
    private fun complete(rule: String, vararg kinds: String) {
        val explorer = TestFixture.explorer(rule).accept(*kinds)
        assertTrue(explorer.isComplete(), "$rule should end after ${kinds.joinToString()}")
    }

    @Test fun modportWithInputAndOutput() = complete("modport_declaration",
        "ModPortKeyword", "Identifier", "OpenParenthesis", "InputKeyword", "Identifier",
        "Comma", "OutputKeyword", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun modportImportsAFunctionName() = complete("modport_declaration",
        "ModPortKeyword", "Identifier", "OpenParenthesis", "ImportKeyword", "Identifier",
        "CloseParenthesis", "Semicolon")

    @Test fun configDefaultLibraryList() = complete("config_rule_statement",
        "DefaultKeyword", "LibListKeyword", "Identifier", "Semicolon")

    @Test fun configInstanceUsesCell() = complete("config_rule_statement",
        "InstanceKeyword", "Identifier", "UseKeyword", "Identifier", "Semicolon")

    @Test fun letDeclarationWithValue() = complete("let_declaration",
        "LetKeyword", "Identifier", "Equals", "IntegerLiteral", "Semicolon")

    @Test fun letDeclarationWithOneFormal() = complete("let_declaration",
        "LetKeyword", "Identifier", "OpenParenthesis", "IntKeyword", "Identifier",
        "CloseParenthesis", "Equals", "Identifier", "Semicolon")

    @Test fun classConstructorWithoutArguments() = complete("class_constructor_declaration",
        "FunctionKeyword", "NewKeyword", "Semicolon", "EndFunctionKeyword")

    @Test fun dynamicArrayNewWithSize() = complete("dynamic_array_new",
        "NewKeyword", "OpenBracket", "IntegerLiteral", "CloseBracket")

    @Test fun classNewWithoutArguments() = complete("class_new", "NewKeyword")

    @Test fun typeReferenceOfLogic() = complete("type_reference",
        "TypeKeyword", "OpenParenthesis", "LogicKeyword", "CloseParenthesis")

    @Test fun coverpointDefaultBin() = complete("cover_point",
        "CoverPointKeyword", "Identifier", "OpenBrace", "BinsKeyword", "Identifier",
        "Equals", "DefaultKeyword", "Semicolon", "CloseBrace")

    @Test fun crossCoverageWithIffCondition() = complete("cover_cross",
        "CrossKeyword", "Identifier", "Comma", "Identifier", "IffKeyword", "OpenParenthesis",
        "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun clockingInputDeclaration() = complete("clocking_item",
        "InputKeyword", "Identifier", "Semicolon")

    @Test fun cycleDelayWithInteger() = complete("cycle_delay", "DoubleHash", "IntegerLiteral")

    @Test fun eventTriggerWithoutDelay() = complete("event_trigger",
        "MinusArrow", "Identifier", "Semicolon")

    @Test fun nonblockingEventTrigger() = complete("event_trigger",
        "MinusDoubleArrow", "Identifier", "Semicolon")

    @Test fun disableForkStatement() = complete("disable_statement",
        "DisableKeyword", "ForkKeyword", "Semicolon")

    @Test fun simpleImmediateAssert() = complete("simple_immediate_assert_statement",
        "AssertKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun simpleImmediateCover() = complete("simple_immediate_cover_statement",
        "CoverKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun assumePropertyStatement() = complete("assume_property_statement",
        "AssumeKeyword", "PropertyKeyword", "OpenParenthesis", "Identifier",
        "CloseParenthesis", "Semicolon")

    @Test fun restrictPropertyStatement() = complete("restrict_property_statement",
        "RestrictKeyword", "PropertyKeyword", "OpenParenthesis", "Identifier",
        "CloseParenthesis", "Semicolon")

    @Test fun expectPropertyStatement() = complete("expect_property_statement",
        "ExpectKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun severityTaskWithoutArguments() = complete("severity_system_task",
        "SystemIdentifier", "Semicolon")

    @Test fun setupTimingCheckWithThreeArguments() = complete("system_timing_check",
        "SystemIdentifier", "OpenParenthesis", "Identifier", "Comma", "Identifier", "Comma",
        "IntegerLiteral", "CloseParenthesis", "Semicolon")

    @Test fun simpleSpecifyPath() = complete("path_declaration",
        "OpenParenthesis", "Identifier", "EqualsArrow", "Identifier", "CloseParenthesis",
        "Equals", "IntegerLiteral", "Semicolon")

    @Test fun streamingConcatenationToTheLeft() = complete("streaming_concatenation",
        "OpenBrace", "LeftShift", "OpenBrace", "Identifier", "CloseBrace", "CloseBrace")

    @Test fun multipleConcatenation() = complete("multiple_concatenation",
        "OpenBrace", "IntegerLiteral", "OpenBrace", "Identifier", "CloseBrace", "CloseBrace")

    @Test fun udpAnsiHeaderHasInputAndOutput() = complete("udp_ansi_declaration",
        "PrimitiveKeyword", "Identifier", "OpenParenthesis", "OutputKeyword", "Identifier",
        "Comma", "InputKeyword", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun udpCombinationalTableEntry() = complete("combinational_body",
        "TableKeyword", "IntegerLiteral", "Colon", "IntegerLiteral", "Semicolon", "EndTableKeyword")

    @Test fun udpSequentialTableEntry() = complete("sequential_body",
        "TableKeyword", "IntegerLiteral", "Colon", "IntegerLiteral", "Colon", "Minus",
        "Semicolon", "EndTableKeyword")

    @Test fun udpAnsiDeclarationWithTable() = complete("udp_declaration",
        "PrimitiveKeyword", "Identifier", "OpenParenthesis", "OutputKeyword", "Identifier",
        "Comma", "InputKeyword", "Identifier", "CloseParenthesis", "Semicolon",
        "TableKeyword", "IntegerLiteral", "Colon", "IntegerLiteral", "Semicolon",
        "EndTableKeyword", "EndPrimitiveKeyword")

    @Test fun udpTableCannotEndBeforeFirstEntry() {
        val explorer = TestFixture.explorer("combinational_body").accept("TableKeyword")
        assertFalse(explorer.advance("EndTableKeyword"))
        assertTrue("IntegerLiteral" in explorer.nextNames())
    }

    @Test fun coverCrossRequiresAtLeastTwoItems() {
        val explorer = TestFixture.explorer("cover_cross").accept("CrossKeyword", "Identifier")
        assertFalse(explorer.advance("Semicolon"))
        assertTrue("Comma" in explorer.nextNames())
    }

    @Test fun modportRequiresNameAfterKeyword() {
        val explorer = TestFixture.explorer("modport_declaration").accept("ModPortKeyword")
        assertFalse(explorer.advance("OpenParenthesis"))
        assertTrue("Identifier" in explorer.nextNames())
    }
}
