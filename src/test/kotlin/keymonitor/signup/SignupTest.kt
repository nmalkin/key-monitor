package keymonitor.signup

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
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
        beforeGroup {
            useNewTestingDatabase()
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
            val newRegistration = processRegistration(registration2)

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

            it("keeps the user's existing email as active") {
                val result = Database.connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails WHERE email='$email'")
                assertTrue(result.next())
                assertEquals(EmailStatus.ACTIVE.name, result.getString("email_status"))
            }

            it("sets the user to be active even if they're currently inactive") {
                val user = getUser(newRegistration.userID)!!
                user.status = UserStatus.DEACTIVATED
                user.save()

                processRegistration(registration2)
                val status = Database.connection.createStatement()
                        .executeQuery("SELECT account_status FROM users WHERE id = ${user.id}")
                        .getString("account_status")

                assertEquals(UserStatus.ACTIVE, UserStatus.valueOf(status))
            }
        }

        on("getting a fully duplicate registration") {
            it("doesn't add a new user to the database") {
                val countBefore = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users")
                        .getInt(1)

                processRegistration(registration2)

                val countAfter = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users")
                        .getInt(1)

                assertEquals(countBefore, countAfter)
            }

            it("doesn't add a new email to the database") {
                val countBefore = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM emails")
                        .getInt(1)

                processRegistration(registration2)

                val countAfter = Database.connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM emails")
                        .getInt(1)

                assertEquals(countBefore, countAfter)
            }

            it("returns the same existing email for each registration attempt") {
                val registration1 = processRegistration(registration2)
                val registration2 = processRegistration(registration2)

                assertEquals(registration1, registration2)
            }

            it("keeps the user's current email address as active") {
                val registration3 = processRegistration(registration2)

                val statusOld = Database.connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails WHERE id = ${registration3.id}")
                        .getString(1)
                assertEquals(EmailStatus.ACTIVE.name, statusOld)
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})