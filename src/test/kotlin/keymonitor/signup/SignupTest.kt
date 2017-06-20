package keymonitor.signup

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useTestingDatabase
import keymonitor.database.Database
import keymonitor.database.EmailStatus
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val phoneNumber = PhoneNumber("+18885550123")
private val email = "test@example.com"
private val registration = RegistrationMessage(phoneNumber, email)
private val email2 = "test2@example.com"
private val registration2 = RegistrationMessage(phoneNumber, email2)

/**
 * Test different scenarios when new or existing numbers & emails are registered
 */
class SignupTest : Spek({
    describe("processing a registration") {
        var testDB: File? = null

        beforeGroup {
            testDB = useTestingDatabase()
        }

        on("getting a new user") {
            processRegistration(registration)

            it("adds a new user to the database") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users")
                assertTrue(result.next())
                assertEquals(1, result.getInt(1))
            }

            it("adds a new email to the database") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM emails")
                assertTrue(result.next())
                assertEquals(1, result.getInt(1))
            }

            it("correctly sets the email status") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails")
                assertTrue(result.next())
                assertEquals(EmailStatus.ACTIVE.name, result.getString("email_status"))
            }
        }

        on("getting an existing user") {
            processRegistration(registration2)

            it("doesn't add a new user to the database") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users")
                assertTrue(result.next())
                assertEquals(1, result.getInt(1))
            }

            it("does add a new email to the database") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM emails")
                assertTrue(result.next())
                assertEquals(2, result.getInt(1))
            }

            it("marks the user's existing email address as having been replaced") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails WHERE email='$email'")
                assertTrue(result.next())
                assertEquals(EmailStatus.REPLACED.name, result.getString("email_status"))
            }
        }

        on("getting a fully duplicate registration") {
            // …it should behave as if a new email has been added
            processRegistration(registration2)

            it("doesn't add a new user to the database") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users")
                assertTrue(result.next())
                assertEquals(1, result.getInt(1))
            }

            it("does add a new email to the database") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM emails")
                assertTrue(result.next())
                assertEquals(3, result.getInt(1))
            }

            it("marks the user's current email address as having been replaced") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails WHERE email='$email2'")
                assertTrue(result.next())
                assertEquals(EmailStatus.REPLACED.name, result.getString("email_status"))
                assertTrue(result.next())
                assertEquals(EmailStatus.ACTIVE.name, result.getString("email_status"))
            }
        }

        afterGroup {
            closeTestingDatabase(testDB)
        }
    }
})