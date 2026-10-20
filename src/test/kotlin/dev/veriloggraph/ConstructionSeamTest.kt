package dev.veriloggraph

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Concrete boundaries from Annex A, checked in one source_text parse context. */
class ConstructionSeamTest {
    private data class Seam(
        val name: String,
        val prefix: List<String>,
        val next: String,
        val startsRule: String,
    )
    private data class EndLabel(val name: String, val prefix: List<String>, val identifierRule: String)

    private fun file(prefix: List<String>): Explorer =
        TestFixture.explorer("source_text").accept(*prefix.toTypedArray())

    private fun Explorer.candidate(kind: String): Candidate =
        assertNotNull(next().firstOrNull { it.token.name == kind },
            "$kind missing after ${prefix.joinToString { it.name }}")

    private fun starts(seam: Seam) {
        val candidate = file(seam.prefix).candidate(seam.next)
        assertContains(candidate.starts, seam.startsRule, seam.name)
        assertTrue(candidate.beginsConstruction, "${seam.name} should begin a new element")
    }

    private val moduleEnd = listOf("ModuleKeyword", "Identifier", "Semicolon", "EndModuleKeyword")
    private val moduleHeader = listOf("ModuleKeyword", "Identifier", "Semicolon")
    private val packageHeader = listOf("PackageKeyword", "Identifier", "Semicolon")
    private val classHeader = listOf("ClassKeyword", "Identifier", "Semicolon")
    private val interfaceHeader = listOf(
        "InterfaceKeyword", "Identifier", "OpenParenthesis", "CloseParenthesis", "Semicolon")
    private val programHeader = listOf(
        "ProgramKeyword", "Identifier", "OpenParenthesis", "CloseParenthesis", "Semicolon")
    private val udpEnd = listOf(
        "PrimitiveKeyword", "Identifier", "OpenParenthesis", "OutputKeyword", "Identifier",
        "Comma", "InputKeyword", "Identifier", "CloseParenthesis", "Semicolon",
        "TableKeyword", "IntegerLiteral", "Colon", "IntegerLiteral", "Semicolon",
        "EndTableKeyword", "EndPrimitiveKeyword")

    @TestFactory fun nextTopLevelDescriptionAfterModule(): List<DynamicTest> = listOf(
        "module" to ("ModuleKeyword" to "module_declaration"),
        "package" to ("PackageKeyword" to "package_declaration"),
        "interface" to ("InterfaceKeyword" to "interface_declaration"),
        "program" to ("ProgramKeyword" to "program_declaration"),
        "primitive" to ("PrimitiveKeyword" to "udp_declaration"),
        "config" to ("ConfigKeyword" to "config_declaration"),
        "class" to ("ClassKeyword" to "class_declaration"),
        "checker" to ("CheckerKeyword" to "checker_declaration"),
        "typedef" to ("TypedefKeyword" to "type_declaration"),
        "bind" to ("BindKeyword" to "bind_directive"),
        "extern" to ("ExternKeyword" to "module_declaration"),
    ).map { (name, pair) ->
        DynamicTest.dynamicTest("endmodule → $name") {
            starts(Seam(name, moduleEnd, pair.first, pair.second))
        }
    }

    @TestFactory fun closingLabelsStayInTheSameDeclaration(): List<DynamicTest> = listOf(
        EndLabel("module", moduleEnd, "module_identifier"),
        EndLabel("interface", interfaceHeader + "EndInterfaceKeyword", "interface_identifier"),
        EndLabel("program", programHeader + "EndProgramKeyword", "program_identifier"),
        EndLabel("package", packageHeader + "EndPackageKeyword", "package_identifier"),
        EndLabel("class", classHeader + "EndClassKeyword", "class_identifier"),
        EndLabel("checker", listOf("CheckerKeyword", "Identifier", "Semicolon", "EndCheckerKeyword"),
            "checker_identifier"),
        EndLabel("config", listOf("ConfigKeyword", "Identifier", "Semicolon", "DesignKeyword",
            "Semicolon", "EndConfigKeyword"), "config_identifier"),
        EndLabel("udp", udpEnd, "udp_identifier"),
    ).map { boundary ->
        DynamicTest.dynamicTest("${boundary.name} : end label") {
            val explorer = file(boundary.prefix)
            val colon = explorer.candidate("Colon")
            assertFalse(colon.beginsConstruction)
            assertTrue(colon.starts.isEmpty())
            assertTrue(explorer.advance("Colon"))
            val label = explorer.candidate("Identifier")
            assertContains(label.starts, boundary.identifierRule)
            assertFalse(label.beginsConstruction)
            assertTrue(explorer.advance("Identifier"))
            assertTrue(explorer.isComplete())
            assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
        }
    }

    @TestFactory fun newModuleItemsAfterHeader(): List<DynamicTest> = listOf(
        "WireKeyword" to "net_declaration",
        "InitialKeyword" to "initial_construct",
        "AlwaysKeyword" to "always_construct",
        "GenerateKeyword" to "generate_region",
        "SpecifyKeyword" to "specify_block",
        "ModuleKeyword" to "module_declaration",
        "InterfaceKeyword" to "interface_declaration",
        "ProgramKeyword" to "program_declaration",
        "AssignKeyword" to "continuous_assign",
        "ParameterKeyword" to "parameter_declaration",
    ).map { (kind, rule) ->
        DynamicTest.dynamicTest("module item → $kind") {
            starts(Seam(kind, moduleHeader, kind, rule))
        }
    }

    @TestFactory fun newPackageItemsAfterHeader(): List<DynamicTest> = listOf(
        "TypedefKeyword" to "type_declaration",
        "FunctionKeyword" to "function_declaration",
        "ClassKeyword" to "class_declaration",
        "CheckerKeyword" to "checker_declaration",
        "LocalParamKeyword" to "local_parameter_declaration",
        "ImportKeyword" to "dpi_import_export",
    ).map { (kind, rule) ->
        DynamicTest.dynamicTest("package item → $kind") {
            starts(Seam(kind, packageHeader, kind, rule))
        }
    }

    @TestFactory fun newClassItemsAfterHeader(): List<DynamicTest> = listOf(
        "FunctionKeyword" to "function_declaration",
        "TaskKeyword" to "task_declaration",
        "ConstraintKeyword" to "constraint_declaration",
        "CoverGroupKeyword" to "covergroup_declaration",
        "LocalParamKeyword" to "local_parameter_declaration",
        "ClassKeyword" to "class_declaration",
    ).map { (kind, rule) ->
        DynamicTest.dynamicTest("class item → $kind") {
            starts(Seam(kind, classHeader, kind, rule))
        }
    }

    @TestFactory fun newInterfaceItemsAfterHeader(): List<DynamicTest> = listOf(
        "GenerateKeyword" to "generate_region",
        "ModPortKeyword" to "modport_declaration",
        "ProgramKeyword" to "program_declaration",
        "InterfaceKeyword" to "interface_declaration",
    ).map { (kind, rule) ->
        DynamicTest.dynamicTest("interface item → $kind") {
            starts(Seam(kind, interfaceHeader, kind, rule))
        }
    }

    @TestFactory fun newProgramItemsAfterHeader(): List<DynamicTest> = listOf(
        "InitialKeyword" to "initial_construct",
        "AssignKeyword" to "continuous_assign",
        "GenerateKeyword" to "generate_region",
    ).map { (kind, rule) ->
        DynamicTest.dynamicTest("program item → $kind") {
            starts(Seam(kind, programHeader, kind, rule))
        }
    }

    @TestFactory fun newItemsAfterPreviousModuleItem(): List<DynamicTest> = listOf(
        Seam("net declaration", moduleHeader + listOf("WireKeyword", "Identifier", "Semicolon"),
            "InitialKeyword", "initial_construct"),
        Seam("initial null statement", moduleHeader + listOf("InitialKeyword", "Semicolon"),
            "AlwaysKeyword", "always_construct"),
        Seam("generate region", moduleHeader + listOf("GenerateKeyword", "EndGenerateKeyword"),
            "SpecifyKeyword", "specify_block"),
        Seam("specify block", moduleHeader + listOf("SpecifyKeyword", "EndSpecifyKeyword"),
            "ModuleKeyword", "module_declaration"),
        Seam("nested module", moduleHeader + moduleEnd,
            "AlwaysKeyword", "always_construct"),
    ).map { seam ->
        DynamicTest.dynamicTest("${seam.name} → next item") {
            val explorer = file(seam.prefix)
            assertFalse(explorer.candidate("EndModuleKeyword").beginsConstruction)
            val candidate = explorer.candidate(seam.next)
            assertContains(candidate.starts, seam.startsRule)
            assertTrue(candidate.beginsConstruction)
        }
    }

    @TestFactory fun closingKeywordsDoNotBeginAnotherConstruction(): List<DynamicTest> = listOf(
        "module" to (moduleHeader to "EndModuleKeyword"),
        "package" to (packageHeader to "EndPackageKeyword"),
        "class" to (classHeader to "EndClassKeyword"),
        "interface" to (interfaceHeader to "EndInterfaceKeyword"),
        "program" to (programHeader to "EndProgramKeyword"),
    ).map { (name, pair) ->
        DynamicTest.dynamicTest("$name closing keyword") {
            assertFalse(file(pair.first).candidate(pair.second).beginsConstruction)
        }
    }

    @TestFactory fun nextItemAfterACompletedItemInOtherContainers(): List<DynamicTest> = listOf(
        Seam("package typedef", packageHeader + listOf("TypedefKeyword", "LogicKeyword", "Identifier", "Semicolon"),
            "FunctionKeyword", "function_declaration"),
        Seam("class method", classHeader + listOf("FunctionKeyword", "Identifier", "Semicolon", "EndFunctionKeyword"),
            "TaskKeyword", "task_declaration"),
        Seam("program initial", programHeader + listOf("InitialKeyword", "Semicolon"),
            "AssignKeyword", "continuous_assign"),
        Seam("interface generate", interfaceHeader + listOf("GenerateKeyword", "EndGenerateKeyword"),
            "ModPortKeyword", "modport_declaration"),
        Seam("udp", udpEnd, "PackageKeyword", "package_declaration"),
    ).map { seam ->
        DynamicTest.dynamicTest("${seam.name} → next item") {
            starts(seam)
        }
    }

    @TestFactory fun repeatedItemsInsideStatementsAndTables(): List<DynamicTest> {
        data class LocalSeam(
            val rule: String,
            val prefix: List<String>,
            val next: String,
            val startsRule: String,
            val close: String,
        )
        return listOf(
            LocalSeam("seq_block", listOf("BeginKeyword", "Semicolon"),
                "IfKeyword", "statement_or_null", "EndKeyword"),
            LocalSeam("case_statement", listOf("CaseKeyword", "OpenParenthesis", "Identifier",
                "CloseParenthesis", "DefaultKeyword", "Colon", "Semicolon"),
                "IntegerLiteral", "case_item", "EndCaseKeyword"),
            LocalSeam("randcase_statement", listOf("RandCaseKeyword", "IntegerLiteral", "Colon", "Semicolon"),
                "IntegerLiteral", "randcase_item", "EndCaseKeyword"),
            LocalSeam("combinational_body", listOf("TableKeyword", "IntegerLiteral", "Colon",
                "IntegerLiteral", "Semicolon"),
                "IntegerLiteral", "combinational_entry", "EndTableKeyword"),
            LocalSeam("covergroup_declaration", listOf("CoverGroupKeyword", "Identifier", "Semicolon",
                "CoverPointKeyword", "Identifier", "Semicolon"),
                "CrossKeyword", "coverage_spec_or_option", "EndGroupKeyword"),
        ).map { seam ->
            DynamicTest.dynamicTest("${seam.rule}: next repeated item") {
                val explorer = TestFixture.explorer(seam.rule).accept(*seam.prefix.toTypedArray())
                val next = explorer.candidate(seam.next)
                assertContains(next.starts, seam.startsRule)
                assertTrue(next.beginsConstruction)
                assertFalse(explorer.candidate(seam.close).beginsConstruction)
            }
        }
    }

    @TestFactory fun tokensFromClosedContainersDoNotLeakIntoTheNextDescription(): List<DynamicTest> = listOf(
        "module" to (moduleEnd to "AlwaysKeyword"),
        "package" to ((packageHeader + "EndPackageKeyword") to "AlwaysKeyword"),
        "interface" to ((interfaceHeader + "EndInterfaceKeyword") to "ModPortKeyword"),
        "program" to ((programHeader + "EndProgramKeyword") to "InitialKeyword"),
        "class" to ((classHeader + "EndClassKeyword") to "EndClassKeyword"),
        "config" to (listOf("ConfigKeyword", "Identifier", "Semicolon", "DesignKeyword",
            "Semicolon", "EndConfigKeyword") to "DesignKeyword"),
        "udp" to (udpEnd to "TableKeyword"),
    ).map { (name, pair) ->
        DynamicTest.dynamicTest("$name closed; ${pair.second} is no longer local") {
            val explorer = file(pair.first)
            assertTrue(explorer.isComplete())
            assertFalse(explorer.next().any { it.token.name == pair.second })
        }
    }

    @Test fun backAcrossEndLabelRestoresTheNextDescriptionBoundary() {
        val explorer = file(moduleEnd)
        assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
        explorer.accept("Colon")
        assertFalse(explorer.candidate("Identifier").beginsConstruction)
        assertTrue(explorer.back())
        assertTrue(explorer.candidate("ModuleKeyword").beginsConstruction)
        assertFalse(explorer.candidate("Colon").beginsConstruction)
    }

    @Test fun isolatedModuleRootCannotReachTopLevelPackageAfterEndmodule() {
        val explorer = TestFixture.explorer("module_declaration").accept(*moduleEnd.toTypedArray())
        assertFalse(explorer.next().any { it.token.name == "PackageKeyword" })
        assertFalse(explorer.candidate("Colon").beginsConstruction)
    }
}
