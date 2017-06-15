package keymonitor.database


import keymonitor.common.PhoneNumber
import keymonitor.database.Database.connection
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


private val phoneNumber = PhoneNumber("+18885550123")
private val address = "test@example.com"

class EmailTest : Spek({
    describe("an email in the database") {
        var tempFile: File? = null
        var user: User? = null
        beforeGroup {
            // Set up a new database for testing
            tempFile = File.createTempFile("testing-database", ".sqlite")
            Database.file = tempFile!!.absolutePath
            setup()

            // Give it a user
            user = createUser(phoneNumber)
        }

        on("adding an email") {
            it("doesn't throw an exception") {
                assertNotNull(addEmail(user!!, address))
            }

            it("correctly increments user IDs") {
                val email = addEmail(user!!, address)
                assertEquals(2, email.id)
            }

            it("results are actually added to the database") {
                addEmail(user!!, address)

                val result = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM emails")
                assertTrue(result.next())
                assertEquals(3, result.getInt(1))
            }

            it("stores the right address") {
                addEmail(user!!, address)

                val result = connection.createStatement()
                        .executeQuery("SELECT email FROM emails")
                while (result.next()) {
                    assertEquals(address.toString(), result.getString("email"))
                }
            }

            it("stores the right initial status") {
                addEmail(user!!, address)

                val result = connection.createStatement()
                        .executeQuery("SELECT email_status FROM emails")
                while (result.next()) {
                    assertEquals(EmailStatus.ACTIVE.name, result.getString("email_status"))
                }
            }

            it("references the right user") {
                val newUser = createUser(phoneNumber)
                val email = addEmail(newUser, address)

                val result = connection.createStatement()
                        .executeQuery("SELECT user FROM emails WHERE id=${email.id}")
                while (result.next()) {
                    assertEquals(newUser.id, result.getInt(1))
                }
            }

            it("creates different unsubscribe tokens for each email") {
                val result = connection.createStatement()
                        .executeQuery("SELECT unsubscribe_token FROM emails")
                val tokens: MutableSet<String> = hashSetOf()
                while (result.next()) {
                    tokens.add(result.getString(1))
                }

                assertEquals(tokens.size, tokens.distinct().size)
            }
        }

        afterGroup {
            // Clean up the testing database
            Database.closeConnection()
            tempFile?.delete()
        }
    }
})
