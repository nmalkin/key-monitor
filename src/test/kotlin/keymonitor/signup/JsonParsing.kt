package keymonitor.signup

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import keymonitor.common.PhoneNumber
import org.apache.commons.validator.routines.EmailValidator
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun assertParsingFails(parsing: () -> Unit) {
    assertFailsWith(JsonParsingException::class, parsing)
}

class JsonParsingTest : Spek({
    describe("signal-cli JSON output parsing") {
        on("parsing an entire file") {
            val testFilePath = JsonParsingTest::class.java.getResource("good_log.txt").toURI()
                ?: throw RuntimeException("can't find test file")
            val goodFile = File(testFilePath)

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

    describe("parsing a single line") {
        val jsonParser = ObjectMapper(JsonFactory())
        val emailValidator = EmailValidator.getInstance()

        it("accepts init debug statements") {
            val result = parseLine(jsonParser, emailValidator, "{\"init\":true},")
            assertNull(result)
        }

        it("accepts done debug statements") {
            val result = parseLine(jsonParser, emailValidator, "{\"done\":true}]")
            assertNull(result)
        }

        it("correctly parses a message") {
            val line = """{"envelope":{"legacyMessage":true,"from":{"number":"+14155550123","device":1},"type":{"number":3,"name":"prekey"},"timestamp":"2017-06-12T23:28:42.630Z"},"data":{"body":" test@example.com  \n\n\n\n","timestamp":1497310122630}},"""
            val result = parseLine(jsonParser, emailValidator, line)
            assertNotNull(result)
            result!!

            assertEquals(PhoneNumber("+14155550123"), result.phoneNumber)
            assertEquals("test@example.com", result.email)
        }

        it("fails if the line isn't JSON") {
            assertParsingFails {
                parseLine(jsonParser, emailValidator, "blah blah blah")
            }
        }

        it("fails if the JSON is missing fields") {
            assertParsingFails {
                val line = """{"data":{"body":" test@example.com  \n\n\n\n","timestamp":1497310122630}},"""
                parseLine(jsonParser, emailValidator, line)
            }

            assertParsingFails {
                val line = """{"envelope":{"legacyMessage":true,"from":{"number":"+14155550123","device":1},"type":{"number":3,"name":"prekey"},"timestamp":"2017-06-12T23:28:42.630Z"}},"""
                parseLine(jsonParser, emailValidator, line)
            }
        }
    }
})
