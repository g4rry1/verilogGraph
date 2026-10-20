package dev.veriloggraph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogTest {
    private val catalog = TestFixture.catalog

    @Test fun bundledCatalogHasEveryPinnedSlangKindExactlyOnce() {
        assertEquals(352, catalog.kinds.size)
        assertEquals(catalog.kinds.size, catalog.kinds.map { it.name }.toSet().size)
        assertEquals(catalog.kinds.size, catalog.byName.size)
    }

    @Test fun knownKeywordsAndPunctuationResolveBySpelling() {
        assertEquals("ModuleKeyword", catalog.bySpelling["module"]?.name)
        assertEquals("EndModuleKeyword", catalog.bySpelling["endmodule"]?.name)
        assertEquals("ApostropheOpenBrace", catalog.bySpelling["'{"]?.name)
        assertEquals("Semicolon", catalog.bySpelling[";"]?.name)
    }

    @Test fun lexicalKindsHaveNoInventedFixedSpelling() {
        for (kind in listOf("Identifier", "IntegerLiteral", "RealLiteral", "IntegerBase")) {
            assertNull(catalog.byName[kind]?.spelling)
        }
    }

    @Test fun spellingLookupRoundTrips() {
        for ((spelling, token) in catalog.bySpelling) {
            assertEquals(spelling, token.spelling)
            assertEquals(token, catalog.byName[token.name])
        }
    }

    @Test fun everyLabelIsNonempty() {
        assertTrue(catalog.kinds.all { it.label.isNotEmpty() })
        assertEquals("Identifier", assertNotNull(catalog.byName["Identifier"]).label)
        assertFalse("DefinitelyNotASlangKind" in catalog.byName)
    }
}
