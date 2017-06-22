package keymonitor.lookup

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class SignalAPITest : Spek({
    describe("loading credentials from file") {
        val validFilename = "+15105550123"
        val validFile = SignalAPITest::class.java.getResource(validFilename).toURI()
        val testDirectory = Paths.get(validFile.path).parent.toString()

        on("loading a valid file") {
            it("correctly retrieves all fields") {
                val credentials = loadSignalCredentialsFromFile(validFilename, testDirectory)

                assertEquals(validFilename, credentials.user)
                assertEquals("This is a test password.", credentials.password)
                assertEquals("This is a test key!", credentials.signalingKey)
            }
        }

        on("loading an invalid file") {
            it("throws an assertion error") {
                val badFile = "+15105550666" // missing signalingKey
                assertFailsWith<JsonParsingException> {
                    loadSignalCredentialsFromFile(badFile, testDirectory)
                }
            }
        }

        on("loading a nonexistent file") {
            it("throws an assertion error") {
                val badFile = "+nope"
                assertFailsWith<JsonParsingException> {
                    loadSignalCredentialsFromFile(badFile, testDirectory)
                }
            }
        }
    }
})