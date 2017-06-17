package keymonitor.common

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import java.io.IOException
import java.net.URLDecoder
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** How long to wait on requests to the mock server before assuming something went wrong */
private val TIMEOUT = 1L

private val address = "test@example.com"
private val subject = "Test message"
private val message = "Hello, world!"
private val sampleResponse = """{
  "message": "Queued. Thank you.",
  "id": "<20111114174239.25659.5817@samples.mailgun.org>"
}"""

class SendEmailTest : Spek({
    describe("sending an email") {
        val server = MockWebServer()

        beforeGroup {
            server.start()

            CONFIGS.EMAIL_DOMAIN = "example.com"
            CONFIGS.MAILGUN_URL = server.url("/").toString()
            CONFIGS.MAILGUN_API_KEY = "test"
        }

        it("makes a request") {
            server.enqueue(MockResponse().setBody(sampleResponse))
            sendMessage(address, subject, message)

            val request = server.takeRequest(TIMEOUT, TimeUnit.SECONDS)
            assertNotNull(request)
        }

        it("throws an exception if the response is 401/unauthorized") {
            server.enqueue(MockResponse().setResponseCode(401))
            assertFailsWith<IOException>("invalid credentials") {
                sendMessage(address, subject, message)
            }
        }

        it("throws an exception if the response is otherwise unsuccessful") {
            server.enqueue(MockResponse().setResponseCode(500))
            assertFailsWith<IOException> {
                sendMessage(address, subject, message)
            }
        }

        it("supplies the correct authorization header") {
            server.enqueue(MockResponse().setBody(sampleResponse))
            sendMessage(address, subject, message)

            val encoding: String = String(Base64.getEncoder().encode("api:test".toByteArray()))
            val expectedHeader = "Basic $encoding"

            val request = server.takeRequest(TIMEOUT, TimeUnit.SECONDS)
            assertEquals(expectedHeader, request.getHeader("Authorization"))
        }

        it("calls the right URL") {
            server.enqueue(MockResponse().setBody(sampleResponse))
            sendMessage(address, subject, message)

            val request = server.takeRequest(TIMEOUT, TimeUnit.SECONDS)
            assertEquals("/v3/${CONFIGS.EMAIL_DOMAIN}/messages", request.path)
        }

        it("includes the right fields in the message") {
            server.enqueue(MockResponse().setBody(sampleResponse))
            sendMessage(address, subject, message)

            val request = server.takeRequest(TIMEOUT, TimeUnit.SECONDS)
            val body = request.body.readUtf8()
            val decoded = URLDecoder.decode(body, "UTF-8")
            val fields = decoded.split("&")
            assertTrue(fields.contains("subject=$subject"))
            assertTrue(fields.contains("text=$message"))
        }

        afterGroup {
            server.shutdown()
        }
    }
})