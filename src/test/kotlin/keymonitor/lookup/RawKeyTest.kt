package keymonitor.lookup

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals


class RawKeyTest : Spek({
    describe("converting a raw key") {
        it("produces the right value") {
            val raw = byteArrayOf(1, 2, 3)
            val encoded = raw.toKeyValue()

            assertEquals("010203", encoded)
        }

        it("encodes it as hex") {
            val raw = byteArrayOf(10, 11, 12)
            val encoded = raw.toKeyValue()

            assertEquals("0A0B0C", encoded)
        }
    }

    describe("converting a key list") {
        it("concatenates values with a string") {
            val encoded = toKeyList(listOf(byteArrayOf(1, 2, 3),
                    byteArrayOf(16, 17, 18)))

            assertEquals("010203,101112", encoded)
        }
    }
})