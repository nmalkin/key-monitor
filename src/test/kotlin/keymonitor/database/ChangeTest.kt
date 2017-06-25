package keymonitor.database


import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.query
import keymonitor.common.useNewTestingDatabase
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


private val phoneNumber = PhoneNumber("+18885550123")
private val someTime = Instant.ofEpochSecond(1_000_000L)!!
private val ip = "127.0.0.1"
private val keyValue = "010203"

class KeyChangeTest : Spek({
    describe("detected key changes in the database") {
        beforeGroup {
            useNewTestingDatabase()
        }

        on("saving a new one") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user, someTime, someTime)
            val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

            it("adds results to the database") {
                val countBefore = query("SELECT COUNT(*) FROM changes").getInt(1)

                saveChange(user.id, key1.id, key2.id)

                val countAfter = query("SELECT COUNT(*) FROM changes").getInt(1)

                assertEquals(countBefore + 1, countAfter)
            }

            it("stores the right values") {
                val change = saveChange(user.id, key1.id, key2.id)

                val result = query("SELECT * FROM changes WHERE id = ${change.id}")
                assertTrue(result.next())

                assertEquals(user.id, result.getInt("user_id"))
                assertEquals(key1.id, result.getInt("last_key"))
                assertEquals(key2.id, result.getInt("new_key"))
            }

            it("stores the correct status") {
                val change = saveChange(user.id, key1.id, key2.id)

                val status = query("SELECT status FROM changes WHERE id = ${change.id}").getString("status")
                assertEquals(KeyChangeStatus.NEW, KeyChangeStatus.valueOf(status))
            }
        }

        on("updating a change") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user, someTime, someTime)
            val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            val change = saveChange(user.id, key1.id, key2.id)

            change.status = KeyChangeStatus.NOTIFIED

            it("changes the value in the database automatically") {
                val status = query("SELECT status FROM changes WHERE id = ${change.id}").getString("status")
                assertEquals(KeyChangeStatus.NOTIFIED, KeyChangeStatus.valueOf(status))
            }
        }

        on("selecting all new changes") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user, someTime, someTime)
            val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            val change1 = saveChange(user.id, key1.id, key2.id)
            val change2 = saveChange(user.id, key2.id, key1.id)
            change2.status = KeyChangeStatus.NOTIFIED
            val change3 = saveChange(user.id, key2.id, key1.id)

            val newChanges = getAllNewChanges()

            it("returns the right keys") {
                assertTrue(newChanges.contains(change1))
                assertTrue(newChanges.contains(change3))
            }

            it("doesn't return the wrong keys") {
                assertFalse(newChanges.contains(change2))
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})
