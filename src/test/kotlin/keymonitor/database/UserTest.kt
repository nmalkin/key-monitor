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
import kotlin.test.assertNull
import kotlin.test.assertTrue


private val phoneNumber = PhoneNumber("+18885550123")

class UserTest : Spek({
    describe("a user in the database") {
        var tempFile: File? = null
        beforeGroup {
            // Set up a new database for testing
            tempFile = File.createTempFile("testing-database", ".sqlite")
            Database.file = tempFile!!.absolutePath
            setup()
        }

        on("creating a user") {
            it("doesn't throw an exception") {
                assertNotNull(createUser(phoneNumber))
            }

            it("correctly increments user IDs") {
                val user = createUser(phoneNumber)
                assertEquals(2, user.id)
            }

            it("results are actually added to the database") {
                createUser(phoneNumber)

                val result = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users")
                assertTrue(result.next())
                assertEquals(3, result.getInt(1))
            }

            it("stores the right number") {
                createUser(phoneNumber)

                val result = connection.createStatement()
                        .executeQuery("SELECT phone FROM users")
                while (result.next()) {
                    assertEquals(phoneNumber.toString(), result.getString("phone"))
                }
            }

            it("stores the right initial status") {
                createUser(phoneNumber)

                val result = connection.createStatement()
                        .executeQuery("SELECT account_status FROM users")
                while (result.next()) {
                    assertEquals(UserStatus.ACTIVE.name, result.getString("account_status"))
                }
            }
        }

        on("getting a user") {
            it("returns the right one") {
                val user = getUser(phoneNumber)
                assertNotNull(user)
                user!!
                assertEquals(1, user.id)
                assertEquals(UserStatus.ACTIVE, user.status)
            }

            it("returns null if it doesn't exist") {
                assertNull(getUser(PhoneNumber("+18885550000")))
            }
        }

        afterGroup {
            // Clean up the testing database
            Database.closeConnection()
            tempFile?.delete()
        }
    }
})
