import keymonitor.common.encodeBytesAsHex
import keymonitor.common.getRandomHex
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue


class RandomTest : Spek({
    it("correctly encodes bytes as hex") {
        val bytes = byteArrayOf(-1, 0, 1, 2, 3)
        assertEquals("FF00010203", encodeBytesAsHex(bytes))
    }

    describe("the random number generator") {
        it("returns new values each time") {
            val r1 = getRandomHex()
            val r2 = getRandomHex()
            assertNotEquals(r1, r2)
        }

        it("produces more randomness when you ask for it") {
            val r1 = getRandomHex()
            val r2 = getRandomHex(17)
            assertTrue(r2.length > r1.length + 1)
        }
    }
})
