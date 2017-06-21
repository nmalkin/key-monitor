package keymonitor.unsubscribe

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.addEmail
import keymonitor.database.createUser
import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.http.HttpMethod
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.testing.handleRequest
import org.jetbrains.ktor.testing.withTestApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @BeforeEach fun setup() {
        useNewTestingDatabase()
    }

    @Test fun testMissingToken() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, "/")) {
            assertEquals("missing token", response.content)
        }
    }

    @Test fun testBadToken() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, "/?t=badtoken")) {
            assertEquals(HttpStatusCode.BadRequest, response.status())
        }
    }

    @Test fun testWorkingToken() = withTestApplication(Application::main) {
        val email = addEmail(createUser(PhoneNumber("+15105550123")), "test@example.com")
        with(handleRequest(HttpMethod.Get, "/?t=${email.unsubscribeToken}")) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @AfterEach fun tearDown() {
        closeTestingDatabase()
    }
}