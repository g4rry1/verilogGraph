package dev.veriloggraph

import java.io.StringReader
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CliTest {
    private fun session(commands: String, rule: String = "module_declaration"): String {
        val output = StringWriter()
        runCli(TestFixture.catalog, TestFixture.grammar, StringReader(commands), output, rule)
        return output.toString()
    }

    @Test fun startupShowsRuleAndPossibleStarts() {
        val output = session("quit\n")
        assertContains(output, "Правило: module_declaration")
        assertContains(output, "ModuleKeyword (module)")
        assertContains(output, "Введите help")
    }

    @Test fun helpListsInteractiveCommands() {
        val output = session("help\nquit\n")
        for (command in listOf("step", "back", "path", "use", "rules", "tokens", "global", "edge")) {
            assertContains(output, command)
        }
    }

    @Test fun stepsAcceptFixedSpellingsAndTokenKindNames() {
        val output = session("step module\nstep Identifier\nstep ;\nstep endmodule\nquit\n")
        assertContains(output, "ModuleKeyword (module) Identifier Semicolon (;) EndModuleKeyword (endmodule)")
        assertContains(output, "Конструкция может завершиться здесь.")
    }

    @Test fun invalidStepDoesNotChangePrefix() {
        val output = session("step module\nstep endmodule\npath\nquit\n")
        assertContains(output, "Недопустимое продолжение: EndModuleKeyword")
        assertContains(output, "Префикс: ModuleKeyword (module)")
        assertFalse(output.contains("Префикс: ModuleKeyword (module) EndModuleKeyword"))
    }

    @Test fun backRestoresPreviousChoices() {
        val output = session("step module\nstep Identifier\nback\nquit\n")
        assertContains(output, "Префикс: ModuleKeyword (module)")
        assertContains(output, "AutomaticKeyword (automatic)")
    }

    @Test fun backAtStartIsReported() {
        assertContains(session("back\nquit\n"), "Префикс уже пуст.")
    }

    @Test fun ruleSwitchEnablesASecondConstruct() {
        val output = session("use generate_region\nstep generate\nstep endgenerate\nquit\n")
        assertContains(output, "Правило: generate_region")
        assertContains(output, "GenerateKeyword (generate) EndGenerateKeyword (endgenerate)")
        assertContains(output, "Конструкция может завершиться здесь.")
    }

    @Test fun lexicalAliasesCanBeSelectedByPublishedName() {
        val output = session("use hex_value\nstep RealLiteral\nquit\n")
        assertContains(output, "Правило: hex_value")
        assertContains(output, "Префикс: RealLiteral")
        assertContains(output, "Конструкция может завершиться здесь.")
    }

    @Test fun initialRuleCanBeChosenBeforeSession() {
        val output = session("step generate\nquit\n", "generate_region")
        assertContains(output, "Правило: generate_region")
        assertContains(output, "GenerateKeyword (generate)")
    }

    @Test fun rulesSearchFindsConstructs() {
        val output = session("rules package_declaration\nquit\n")
        assertContains(output, "Найдено: 1")
        assertContains(output, "package_declaration")
    }

    @Test fun tokenSearchFindsKindsAndSpellings() {
        val output = session("tokens endgenerate\nquit\n")
        assertContains(output, "EndGenerateKeyword (endgenerate)")
    }

    @Test fun ruleCommandShowsAnnexText() {
        val output = session("rule generate_region\nquit\n")
        assertContains(output, "generate_region ::=")
        assertContains(output, "A.4.2 Generated instantiation")
    }

    @Test fun globalCommandListsEdgesWithoutChangingContext() {
        val output = session("global module\npath\nquit\n")
        assertContains(output, "Глобальные продолжения ModuleKeyword (module):")
        assertContains(output, "AutomaticKeyword (automatic)")
        assertContains(output, "Префикс: (пусто)")
    }

    @Test fun nextCanFilterLargeContinuationSet() {
        val output = session("step module\nstep Identifier\nstep ;\nnext endmodule\nquit\n")
        assertContains(output, "Продолжений: 1")
        assertContains(output, "EndModuleKeyword (endmodule)")
    }

    @Test fun globalCanFilterWithoutChangingPrefix() {
        val output = session("global module static\npath\nquit\n")
        assertContains(output, "StaticKeyword (static)")
        assertContains(output, "Префикс: (пусто)")
    }

    @Test fun edgeCommandReportsAnnexProvenance() {
        val output = session("edge RealLiteral Identifier\nquit\n")
        assertContains(output, "RealLiteral → Identifier")
        assertContains(output, "hex_value — A.8.7 Numbers")
    }

    @Test fun edgeCommandReportsMissingEdge() {
        val output = session("edge module module\nquit\n")
        assertContains(output, "Глобального ребра нет")
    }

    @Test fun errorsRemainRecoverable() {
        val output = session("step\nstep NoSuchToken\nuse NoSuchRule\nedge foo\nwhat\nstep module\nquit\n")
        assertContains(output, "Использование: step")
        assertContains(output, "Неизвестный токен: NoSuchToken")
        assertContains(output, "Правило недоступно")
        assertContains(output, "Использование: edge")
        assertContains(output, "Неизвестная команда: what")
        assertContains(output, "Префикс: ModuleKeyword (module)")
    }

    @Test fun quitStopsReadingFurtherCommands() {
        val output = session("quit\nstep module\n")
        assertContains(output, "Выход.")
        assertFalse(output.contains("Префикс: ModuleKeyword (module)"))
    }

    @Test fun eofEndsWithoutError() {
        assertTrue(session("").startsWith("VerilogGraph CLI"))
    }
}
