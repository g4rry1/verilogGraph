package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** End-to-end paths taken from distinct Annex A constructions, using slang TokenKind names. */
class ConstructGrammarTest {
    private fun complete(rule: String, vararg kinds: String) {
        val explorer = TestFixture.explorer(rule).accept(*kinds)
        assertTrue(explorer.isComplete(), "$rule should end after ${kinds.joinToString()}")
    }

    @Test fun emptyPackageDeclaration() = complete("package_declaration",
        "PackageKeyword", "Identifier", "Semicolon", "EndPackageKeyword")

    @Test fun packageWithAutomaticLifetime() = complete("package_declaration",
        "PackageKeyword", "AutomaticKeyword", "Identifier", "Semicolon", "EndPackageKeyword")

    @Test fun emptyInterfaceDeclaration() = complete("interface_declaration",
        "InterfaceKeyword", "Identifier", "OpenParenthesis", "CloseParenthesis",
        "Semicolon", "EndInterfaceKeyword")

    @Test fun emptyProgramDeclaration() = complete("program_declaration",
        "ProgramKeyword", "Identifier", "OpenParenthesis", "CloseParenthesis",
        "Semicolon", "EndProgramKeyword")

    @Test fun emptyCheckerDeclaration() = complete("checker_declaration",
        "CheckerKeyword", "Identifier", "Semicolon", "EndCheckerKeyword")

    @Test fun emptyClassDeclaration() = complete("class_declaration",
        "ClassKeyword", "Identifier", "Semicolon", "EndClassKeyword")

    @Test fun virtualClassDeclaration() = complete("class_declaration",
        "VirtualKeyword", "ClassKeyword", "Identifier", "Semicolon", "EndClassKeyword")

    @Test fun emptyInterfaceClassDeclaration() = complete("interface_class_declaration",
        "InterfaceKeyword", "ClassKeyword", "Identifier", "Semicolon", "EndClassKeyword")

    @Test fun emptyTaskDeclaration() = complete("task_declaration",
        "TaskKeyword", "Identifier", "Semicolon", "EndTaskKeyword")

    @Test fun emptyFunctionDeclaration() = complete("function_declaration",
        "FunctionKeyword", "Identifier", "Semicolon", "EndFunctionKeyword")

    @Test fun emptyGenerateRegion() = complete("generate_region",
        "GenerateKeyword", "EndGenerateKeyword")

    @Test fun emptySpecifyBlock() = complete("specify_block",
        "SpecifyKeyword", "EndSpecifyKeyword")

    @Test fun emptyCovergroupDeclaration() = complete("covergroup_declaration",
        "CoverGroupKeyword", "Identifier", "Semicolon", "EndGroupKeyword")

    @Test fun emptySequentialBlock() = complete("seq_block",
        "BeginKeyword", "EndKeyword")

    @Test fun timeunitDeclaration() = complete("timeunits_declaration",
        "TimeUnitKeyword", "TimeLiteral", "Semicolon")

    @Test fun timeprecisionDeclaration() = complete("timeunits_declaration",
        "TimePrecisionKeyword", "TimeLiteral", "Semicolon")

    @Test fun genvarDeclaration() = complete("genvar_declaration",
        "GenVarKeyword", "Identifier", "Semicolon")

    @Test fun packageImportDeclaration() = complete("package_import_declaration",
        "ImportKeyword", "Identifier", "DoubleColon", "Identifier", "Semicolon")

    @Test fun simpleDataDeclaration() = complete("data_declaration",
        "LogicKeyword", "Identifier", "Semicolon")

    @Test fun simpleNetDeclaration() = complete("net_declaration",
        "WireKeyword", "Identifier", "Semicolon")

    @Test fun moduleInstantiationWithNoPorts() = complete("module_instantiation",
        "Identifier", "Identifier", "OpenParenthesis", "CloseParenthesis", "Semicolon")

    @Test fun initialBlockMayContainNullStatement() = complete("initial_construct",
        "InitialKeyword", "Semicolon")

    @Test fun alwaysCombMayContainEmptySequentialBlock() = complete("always_construct",
        "AlwaysCombKeyword", "BeginKeyword", "EndKeyword")

    @Test fun continuousNetAssignment() = complete("continuous_assign",
        "AssignKeyword", "Identifier", "Equals", "IntegerLiteral", "Semicolon")

    @Test fun nonblockingAssignment() = complete("nonblocking_assignment",
        "Identifier", "LessThanEquals", "IntegerLiteral")

    @Test fun conditionalWithNullBranch() = complete("conditional_statement",
        "IfKeyword", "OpenParenthesis", "IntegerLiteral", "CloseParenthesis", "Semicolon")

    @Test fun caseWithDefaultItem() = complete("case_statement",
        "CaseKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis",
        "DefaultKeyword", "Colon", "Semicolon", "EndCaseKeyword")

    @Test fun repeatLoopWithNullBody() = complete("loop_statement",
        "RepeatKeyword", "OpenParenthesis", "IntegerLiteral", "CloseParenthesis", "Semicolon")

    @Test fun basicAssertionStatement() = complete("assert_property_statement",
        "AssertKeyword", "PropertyKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun attributeHasCompositeDelimiters() = complete("attribute_instance",
        "OpenParenthesis", "Star", "Identifier", "Star", "CloseParenthesis")

    @Test fun emptyParallelBlock() = complete("par_block", "ForkKeyword", "JoinKeyword")

    @Test fun foreverLoopWithNullBody() = complete("loop_statement", "ForeverKeyword", "Semicolon")

    @Test fun forLoopWithOmittedClauses() = complete("loop_statement",
        "ForKeyword", "OpenParenthesis", "Semicolon", "Semicolon", "CloseParenthesis", "Semicolon")

    @Test fun randcaseWithOneItem() = complete("randcase_statement",
        "RandCaseKeyword", "IntegerLiteral", "Colon", "Semicolon", "EndCaseKeyword")

    @Test fun emptyStaticConstraintBlock() = complete("constraint_declaration",
        "StaticKeyword", "ConstraintKeyword", "Identifier", "OpenBrace", "CloseBrace")

    @Test fun coverpointWithNoBins() = complete("cover_point",
        "CoverPointKeyword", "Identifier", "Semicolon")

    @Test fun propertyDeclarationWithSimpleExpression() = complete("property_declaration",
        "PropertyKeyword", "Identifier", "Semicolon", "Identifier", "Semicolon", "EndPropertyKeyword")

    @Test fun sequenceDeclarationWithSimpleExpression() = complete("sequence_declaration",
        "SequenceKeyword", "Identifier", "Semicolon", "Identifier", "Semicolon", "EndSequenceKeyword")

    @Test fun configurationWithEmptyDesignList() = complete("config_declaration",
        "ConfigKeyword", "Identifier", "Semicolon", "DesignKeyword", "Semicolon", "EndConfigKeyword")

    @Test fun typedefOfLogic() = complete("type_declaration",
        "TypedefKeyword", "LogicKeyword", "Identifier", "Semicolon")

    @Test fun clockingEventWithNamedSignal() = complete("clocking_event", "At", "Identifier")

    @Test fun returnWithoutValue() = complete("jump_statement", "ReturnKeyword", "Semicolon")

    @Test fun waitWithNullBody() = complete("wait_statement",
        "WaitKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun externPrimitiveDeclarationHasHeader() {
        val explorer = TestFixture.explorer("udp_declaration").accept(
            "ExternKeyword", "PrimitiveKeyword", "Identifier", "OpenParenthesis"
        )
        assertTrue("Identifier" in explorer.nextNames())
        assertFalse(explorer.isComplete())
    }

    @Test fun caseStatementRequiresAtLeastOneItem() {
        val explorer = TestFixture.explorer("case_statement").accept(
            "CaseKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis"
        )
        assertFalse(explorer.advance("EndCaseKeyword"))
        assertTrue("DefaultKeyword" in explorer.nextNames())
    }

    @Test fun classNeedsNameBeforeSemicolon() {
        val explorer = TestFixture.explorer("class_declaration").accept("ClassKeyword")
        assertFalse(explorer.advance("Semicolon"))
        assertEquals("ClassKeyword", explorer.prefix.single().name)
    }

    @Test fun functionNeedsNameBeforeEndKeyword() {
        val explorer = TestFixture.explorer("function_declaration").accept("FunctionKeyword")
        assertFalse(explorer.advance("EndFunctionKeyword"))
    }
}
