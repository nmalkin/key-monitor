package keymonitor.database

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.Database.connection
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue


private val phoneNumber = PhoneNumber("+18885550123")

class UserTest : Spek({
    describe("a user in the database") {
        beforeGroup {
            useNewTestingDatabase()
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

        on("getting a user by their phone number") {
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

        on("getting a user by their ID") {
            it("returns the right one") {
                val createdUser = createUser(phoneNumber)
                val user = getUser(createdUser.id)
                assertNotNull(user)
                user!!
                assertEquals(createdUser.phoneNumber, user.phoneNumber)
                assertEquals(createdUser.status, user.status)
            }

            it("returns null if it doesn't exist") {
                assertNull(getUser(PhoneNumber("+18885550000")))
            }
        }

        on("updating a user's status") {
            it("changes the value in the database") {
                val user = getUser(phoneNumber)
                user!!
                user.status = UserStatus.DEACTIVATED
                user.save()

                val result = connection.createStatement()
                        .executeQuery("SELECT account_status FROM users WHERE id = ${user.id}")
                assertTrue(result.next())
                assertEquals(user.status, UserStatus.valueOf(result.getString("account_status")))
            }
        }

        on("getting all active users") {
            it("returns the right number of users") {
                val activeUsers = getActiveUsers()

                val count = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM users WHERE account_status = '${UserStatus.ACTIVE.name}'")
                        .getInt(1)

                assertEquals(count, activeUsers.size)
            }

            it("contains identifiable users") {
                val newUser = createUser(PhoneNumber("+14155550101"))
                val activeUsers = getActiveUsers()
                assertTrue(activeUsers.contains(newUser))
            }

            it("doesn't return anyone who isn't active") {
                // Make sure there's at least one inactive user
                val inactiveUser = createUser(PhoneNumber("+14155550102"))
                inactiveUser.status = UserStatus.DEACTIVATED
                inactiveUser.save()

                val activeUsers = getActiveUsers()

                val result = connection.createStatement()
                        .executeQuery("SELECT id FROM users WHERE account_status = '${UserStatus.DEACTIVATED.name}'")
                while (result.next()) {
                    val userID = result.getInt("id")
                    val user = getUser(userID)!!
                    assertTrue(!activeUsers.contains(user))
                }
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})
