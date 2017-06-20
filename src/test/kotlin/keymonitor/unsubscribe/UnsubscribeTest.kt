package keymonitor.unsubscribe

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useTestingDatabase
import keymonitor.database.*
import keymonitor.database.Database.connection
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Create a new user and new email for testing, returning the latter */
private fun newTestingEmail(): Email {
    return addEmail(createUser(PhoneNumber("+15105550123")), "test@example.com")
}

/**
 * Test the unsubscribe logic
 */
class UnsubscribeTest : Spek({
    describe("handling an unsubscribe request") {
        var testDB: File? = null

        beforeGroup {
            testDB = useTestingDatabase()
        }

        on("receiving a valid token") {
            it("returns success") {
                val unsubscribe = processUnsubscribe(newTestingEmail().unsubscribeToken)

                assertEquals(UnsubscribeResult.SUCCESS, unsubscribe)
            }
            it("marks the email as unsubscribed in the database") {
                val email = newTestingEmail()
                processUnsubscribe(email.unsubscribeToken)

                val result = connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails WHERE id = ${email.id}")
                assertTrue(result.next())
                assertEquals(EmailStatus.UNSUBSCRIBED,
                        EmailStatus.valueOf(result.getString("email_status")))
            }

            it("marks the account as inactive in the database") {
                val email = addEmail(createUser(PhoneNumber("+15105550123")), "test@example.com")
                processUnsubscribe(email.unsubscribeToken)

                val result = connection.createStatement()
                        .executeQuery("SELECT account_status FROM users WHERE id = ${email.user.id}")
                assertTrue(result.next())
                assertEquals(UserStatus.DEACTIVATED,
                        UserStatus.valueOf(result.getString("account_status")))
            }
        }

        on("receiving an invalid token") {
            it("says that it failed") {
                val unsubscribe = processUnsubscribe("this token does not exist")
                assertEquals(UnsubscribeResult.FAIL, unsubscribe)
            }
        }

        afterGroup {
            closeTestingDatabase(testDB)
        }
    }
})