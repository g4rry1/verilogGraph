package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Longer examples exercise optional parts, repetitions and alternatives of Annex A rules. */
class DetailedConstructGrammarTest {
    private fun complete(rule: String, vararg kinds: String) {
        val explorer = TestFixture.explorer(rule).accept(*kinds)
        assertTrue(explorer.isComplete(), "$rule should end after ${kinds.joinToString()}")
    }

    @Test fun moduleWithTwoAnsiPorts() = complete("module_declaration",
        "ModuleKeyword", "Identifier", "OpenParenthesis",
        "InputKeyword", "LogicKeyword", "Identifier", "Comma",
        "OutputKeyword", "LogicKeyword", "Identifier", "CloseParenthesis",
        "Semicolon", "EndModuleKeyword")

    @Test fun moduleWithIntegerParameterAndEmptyPorts() = complete("module_declaration",
        "ModuleKeyword", "Identifier", "Hash", "OpenParenthesis",
        "ParameterKeyword", "IntKeyword", "Identifier", "Equals", "IntegerLiteral",
        "CloseParenthesis", "OpenParenthesis", "CloseParenthesis",
        "Semicolon", "EndModuleKeyword")

    @Test fun moduleWithEndLabel() = complete("module_declaration",
        "ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword", "Colon", "Identifier")

    @Test fun packageWithTimeunitDeclaration() = complete("package_declaration",
        "PackageKeyword", "Identifier", "Semicolon", "TimeUnitKeyword", "TimeLiteral",
        "Semicolon", "EndPackageKeyword")

    @Test fun classExtendsAnotherClass() = complete("class_declaration",
        "ClassKeyword", "Identifier", "ExtendsKeyword", "Identifier", "Semicolon", "EndClassKeyword")

    @Test fun classImplementsAnInterfaceClass() = complete("class_declaration",
        "ClassKeyword", "Identifier", "ImplementsKeyword", "Identifier", "Semicolon", "EndClassKeyword")

    @Test fun functionWithExplicitLogicReturnType() = complete("function_declaration",
        "FunctionKeyword", "LogicKeyword", "Identifier", "Semicolon", "EndFunctionKeyword")

    @Test fun taskWithOneInputPort() = complete("task_declaration",
        "TaskKeyword", "Identifier", "OpenParenthesis", "InputKeyword", "LogicKeyword", "Identifier",
        "CloseParenthesis", "Semicolon", "EndTaskKeyword")

    @Test fun initializedLogicVariable() = complete("data_declaration",
        "LogicKeyword", "Identifier", "Equals", "IntegerLiteral", "Semicolon")

    @Test fun unpackedArrayVariable() = complete("data_declaration",
        "LogicKeyword", "Identifier", "OpenBracket", "IntegerLiteral", "Colon", "IntegerLiteral",
        "CloseBracket", "Semicolon")

    @Test fun integerParameterWithDefault() = complete("parameter_declaration",
        "ParameterKeyword", "IntKeyword", "Identifier", "Equals", "IntegerLiteral")

    @Test fun forwardClassTypedef() = complete("type_declaration",
        "TypedefKeyword", "ClassKeyword", "Identifier", "Semicolon")

    @Test fun wildcardPackageImport() = complete("package_import_declaration",
        "ImportKeyword", "Identifier", "DoubleColon", "Star", "Semicolon")

    @Test fun moduleInstantiationWithOrderedConnections() = complete("module_instantiation",
        "Identifier", "Identifier", "OpenParenthesis", "Identifier", "Comma", "Identifier",
        "CloseParenthesis", "Semicolon")

    @Test fun moduleInstantiationWithNamedConnection() = complete("module_instantiation",
        "Identifier", "Identifier", "OpenParenthesis", "Dot", "Identifier", "OpenParenthesis",
        "Identifier", "CloseParenthesis", "CloseParenthesis", "Semicolon")

    @Test fun moduleInstantiationWithNamedParameterOverride() = complete("module_instantiation",
        "Identifier", "Hash", "OpenParenthesis", "Dot", "Identifier", "OpenParenthesis",
        "IntegerLiteral", "CloseParenthesis", "CloseParenthesis", "Identifier",
        "OpenParenthesis", "CloseParenthesis", "Semicolon")

    @Test fun generateIfElseWithEmptyBlocks() = complete("if_generate_construct",
        "IfKeyword", "OpenParenthesis", "IntegerLiteral", "CloseParenthesis",
        "BeginKeyword", "EndKeyword", "ElseKeyword", "BeginKeyword", "EndKeyword")

    @Test fun generateCaseWithDefaultBlock() = complete("case_generate_construct",
        "CaseKeyword", "OpenParenthesis", "IntegerLiteral", "CloseParenthesis",
        "DefaultKeyword", "Colon", "BeginKeyword", "EndKeyword", "EndCaseKeyword")

    @Test fun ifElseWithNullStatements() = complete("conditional_statement",
        "IfKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon",
        "ElseKeyword", "Semicolon")

    @Test fun casezWithNumericAndDefaultItems() = complete("case_statement",
        "CaseZKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis",
        "IntegerLiteral", "Colon", "Semicolon", "DefaultKeyword", "Colon", "Semicolon",
        "EndCaseKeyword")

    @Test fun whileLoopWithNullBody() = complete("loop_statement",
        "WhileKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis", "Semicolon")

    @Test fun doWhileLoopWithNullBody() = complete("loop_statement",
        "DoKeyword", "Semicolon", "WhileKeyword", "OpenParenthesis", "Identifier",
        "CloseParenthesis", "Semicolon")

    @Test fun parallelBlockJoinAny() = complete("par_block", "ForkKeyword", "JoinAnyKeyword")

    @Test fun parallelBlockJoinNone() = complete("par_block", "ForkKeyword", "JoinNoneKeyword")

    @Test fun defaultClockingBlock() = complete("clocking_declaration",
        "DefaultKeyword", "ClockingKeyword", "Identifier", "At", "Identifier", "Semicolon",
        "EndClockingKeyword")

    @Test fun covergroupWithOneCoverpoint() = complete("covergroup_declaration",
        "CoverGroupKeyword", "Identifier", "Semicolon", "CoverPointKeyword", "Identifier",
        "Semicolon", "EndGroupKeyword")

    @Test fun crossCoverageOfTwoPoints() = complete("cover_cross",
        "CrossKeyword", "Identifier", "Comma", "Identifier", "Semicolon")

    @Test fun constraintWithAComparison() = complete("constraint_declaration",
        "ConstraintKeyword", "Identifier", "OpenBrace", "Identifier", "GreaterThan",
        "IntegerLiteral", "Semicolon", "CloseBrace")

    @Test fun propertyWithDisableIff() = complete("property_declaration",
        "PropertyKeyword", "Identifier", "Semicolon", "DisableKeyword", "IffKeyword",
        "OpenParenthesis", "Identifier", "CloseParenthesis", "Identifier", "Semicolon",
        "EndPropertyKeyword")

    @Test fun propertySequenceImplication() = complete("property_expr",
        "Identifier", "OrMinusArrow", "Identifier")

    @Test fun eventExpressionWithPosedge() = complete("event_expression",
        "PosEdgeKeyword", "Identifier")

    @Test fun configurationWithDesignCell() = complete("config_declaration",
        "ConfigKeyword", "Identifier", "Semicolon", "DesignKeyword", "Identifier", "Dot",
        "Identifier", "Semicolon", "EndConfigKeyword")

    @Test fun attributeWithConstantValue() = complete("attribute_instance",
        "OpenParenthesis", "Star", "Identifier", "Equals", "IntegerLiteral",
        "Star", "CloseParenthesis")

    @Test fun assignmentPatternWithTwoElements() = complete("assignment_pattern",
        "ApostropheOpenBrace", "IntegerLiteral", "Comma", "IntegerLiteral", "CloseBrace")

    @Test fun concatenationWithTwoExpressions() = complete("concatenation",
        "OpenBrace", "Identifier", "Comma", "Identifier", "CloseBrace")

    @Test fun netAliasOfTwoNames() = complete("net_alias",
        "AliasKeyword", "Identifier", "Equals", "Identifier", "Semicolon")

    @Test fun specparamWithConstantValue() = complete("specparam_declaration",
        "SpecParamKeyword", "Identifier", "Equals", "IntegerLiteral", "Semicolon")

    @Test fun dpiFunctionImport() = complete("dpi_import_export",
        "ImportKeyword", "StringLiteral", "FunctionKeyword", "IntKeyword", "Identifier", "Semicolon")

    @Test fun ifCannotEndBeforeItsBody() {
        val explorer = TestFixture.explorer("conditional_statement").accept(
            "IfKeyword", "OpenParenthesis", "Identifier", "CloseParenthesis"
        )
        assertFalse(explorer.isComplete())
        assertFalse(explorer.advance("ElseKeyword"))
    }

    @Test fun namedPortMustHaveIdentifierAfterDot() {
        val explorer = TestFixture.explorer("named_port_connection").accept("Dot")
        assertFalse(explorer.advance("CloseParenthesis"))
        assertTrue("Identifier" in explorer.nextNames())
    }
}
