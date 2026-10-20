package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NumericGrammarTest {
    @Test fun everyBasedNumberStartsWithOptionalSizeOrBase() {
        for (rule in listOf("binary_number", "octal_number", "hex_number")) {
            assertEquals(setOf("IntegerLiteral", "IntegerBase"), TestFixture.explorer(rule).nextNames())
        }
    }

    @Test fun binaryAndOctalValuesDoNotStartWithRealLiteral() {
        for (rule in listOf("binary_number", "octal_number")) {
            val explorer = TestFixture.explorer(rule).accept("IntegerBase")
            assertEquals(setOf("IntegerLiteral", "Identifier", "Question"), explorer.nextNames())
        }
    }

    @Test fun hexadecimalValueCanStartWithRealLiteral() {
        val explorer = TestFixture.explorer("hex_number").accept("IntegerBase")
        assertEquals(setOf("IntegerLiteral", "Identifier", "Question", "RealLiteral"), explorer.nextNames())
    }

    @Test fun binaryValueCanSpanFourKinds() {
        val explorer = TestFixture.explorer("binary_number").accept(
            "IntegerBase", "IntegerLiteral", "Identifier", "Question", "IntegerLiteral"
        )
        assertTrue(explorer.isComplete()) // 'b1x?0
    }

    @Test fun octalValueCanSpanFourKinds() {
        val explorer = TestFixture.explorer("octal_number").accept(
            "IntegerBase", "IntegerLiteral", "Identifier", "Question", "IntegerLiteral"
        )
        assertTrue(explorer.isComplete()) // 'o7z?3
    }

    @Test fun hexadecimalExponentLookalikeIsAValueKind() {
        val explorer = TestFixture.explorer("hex_number").accept(
            "IntegerBase", "RealLiteral", "Identifier", "Question", "Identifier"
        )
        assertTrue(explorer.isComplete()) // 'h1e2a?f
    }

    @Test fun sizedHexadecimalCanContinueAfterQuestion() {
        val explorer = TestFixture.explorer("hex_number").accept(
            "IntegerLiteral", "IntegerBase", "Question", "RealLiteral", "Question", "IntegerLiteral"
        )
        assertTrue(explorer.isComplete()) // 32'h?1e2?3
    }

    @Test fun questionCanRepeatWithoutBound() {
        val explorer = TestFixture.explorer("@lexer_binary_value")
        repeat(8) { explorer.accept("Question") }
        assertTrue(explorer.isComplete())
        assertTrue("IntegerLiteral" in explorer.nextNames())
    }

    @Test fun adjacentIdentifierCannotBeFollowedByNumericKindInsideValue() {
        for (rule in listOf("@lexer_binary_value", "@lexer_octal_value", "@lexer_hex_value")) {
            val explorer = TestFixture.explorer(rule).accept("Identifier")
            assertEquals(setOf("Question"), explorer.nextNames())
            assertFalse(explorer.advance("IntegerLiteral"))
        }
    }

    @Test fun numericTokenCanBeFollowedByUnknownDigit() {
        for (rule in listOf("@lexer_binary_value", "@lexer_octal_value", "@lexer_hex_value")) {
            val explorer = TestFixture.explorer(rule).accept("IntegerLiteral")
            assertEquals(setOf("Identifier", "Question"), explorer.nextNames())
        }
    }

    @Test fun decimalQuestionMayHaveTrailingUnderscoreChunk() {
        val explorer = TestFixture.explorer("@lexer_decimal_number").accept(
            "IntegerBase", "Question", "Identifier"
        )
        assertTrue(explorer.isComplete())
    }

    @Test fun decimalDoesNotAllowASecondNumericChunk() {
        val explorer = TestFixture.explorer("@lexer_decimal_number").accept("IntegerBase", "IntegerLiteral")
        assertTrue(explorer.isComplete())
        assertFalse(explorer.advance("IntegerLiteral"))
    }

    @Test fun basedNumberCannotStopImmediatelyAfterBase() {
        for (rule in listOf("binary_number", "octal_number", "hex_number", "@lexer_decimal_number")) {
            assertFalse(TestFixture.explorer(rule).accept("IntegerBase").isComplete())
        }
    }

    @Test fun numberUnionIncludesHexOnlyRealLiteralAfterBase() {
        val explorer = TestFixture.explorer("number").accept("IntegerBase")
        assertEquals(setOf("IntegerLiteral", "Identifier", "Question", "RealLiteral"), explorer.nextNames())
    }
}
