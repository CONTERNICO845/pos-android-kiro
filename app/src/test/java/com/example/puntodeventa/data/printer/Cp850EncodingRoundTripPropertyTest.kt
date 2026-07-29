package com.example.puntodeventa.data.printer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.nio.charset.Charset

/**
 * Property-based test for Property 4: CP850 Encoding Round-Trip for Spanish Characters.
 *
 * For any string containing characters in the CP850 character set (including ñ, á, é, í, ó, ú, ¡, ¿),
 * encoding with Charset.forName("Cp850") and decoding back with the same charset produces the original string.
 *
 * **Validates: Requirements 4.1, 4.2**
 */
class Cp850EncodingRoundTripPropertyTest : FunSpec({

    /**
     * Characters known to be in the CP850 code page:
     * - ASCII printable (letters, digits, basic punctuation)
     * - Spanish accented characters (lowercase and uppercase)
     * - Spanish special characters (¡, ¿, ü, Ü)
     */
    val cp850Chars: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') +
        listOf(
            'ñ', 'Ñ', 'á', 'é', 'í', 'ó', 'ú',
            'Á', 'É', 'Í', 'Ó', 'Ú', 'ü', 'Ü',
            '¡', '¿', ' ', '.', ','
        )

    /**
     * Custom arbitrary that generates strings of length 0..100 composed only
     * of CP850-compatible characters (ASCII printable + Spanish accented).
     */
    val arbCp850String: Arb<String> = arbitrary { rs ->
        val length = Arb.int(0..100).bind()
        buildString {
            repeat(length) {
                append(cp850Chars[rs.random.nextInt(cp850Chars.size)])
            }
        }
    }

    /**
     * **Feature: lan-printer-connection, Property 4: CP850 Encoding Round-Trip**
     *
     * For any string composed of CP850-compatible characters, encoding the string
     * with Charset.forName("Cp850") and decoding the resulting bytes back with the
     * same charset must produce the original string.
     *
     * **Validates: Requirements 4.1, 4.2**
     */
    test("Feature: lan-printer-connection, Property 4: CP850 Encoding Round-Trip") {
        val cp850 = Charset.forName("Cp850")

        checkAll(PropTestConfig(iterations = 100), arbCp850String) { original ->
            val encoded = original.toByteArray(cp850)
            val decoded = String(encoded, cp850)
            decoded shouldBe original
        }
    }
})
