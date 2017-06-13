import keymonitor.PhoneNumber
import keymonitor.signup.JsonParsingException
import keymonitor.signup.RegistrationMessage
import keymonitor.signup.parseJsonFile
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

fun assertParsingFails(parsing: () -> Unit) {
    assertFailsWith(JsonParsingException::class, parsing)
}

class JsonParsingTest : Spek({
    describe("signal-cli JSON output parsing") {
        on("parsing an entire file") {
            val goodFile = File(JsonParsingTest::class.java.getResource("good_log.txt")?.toURI())
            val messages = parseJsonFile(goodFile)

            it("is able to parse it into messages") {
                assertNotNull(messages)
            }

            it("gets the right number of messages") {
                assertEquals(2, messages.size)
            }

            it("extracts the phone number from a message") {
                val aMessage: RegistrationMessage = messages.take(1)[0]
                assertEquals(PhoneNumber("+14155550123"), aMessage.phoneNumber)
            }

            it("extracts the email from a message") {
                val aMessage: RegistrationMessage = messages.take(1)[0]
                assertEquals("test@example.com", aMessage.email)
            }
        }
    }
})