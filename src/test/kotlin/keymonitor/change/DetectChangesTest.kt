package keymonitor.change

import keymonitor.common.PhoneNumber
import keymonitor.common.query
import keymonitor.database.*
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
private val keyValue2 = "A0B0C0"

class DetectChangesTest : Spek({
    describe("checking if keys changed") {
        on("getting a key that hasn't changed") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user.id, someTime, someTime)
            saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

            it("returns false") {
                val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                assertFalse(checkForChanges(key2))
            }

            it("doesn't save any change objects") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                val countBefore = query("SELECT COUNT(*) FROM changes").getInt(1)
                checkForChanges(keyNew)
                val countAfter = query("SELECT COUNT(*) FROM changes").getInt(1)
                assertEquals(countBefore, countAfter)
            }

            it("marks the key as having been checked") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                assertEquals(KeyStatus.UNCHECKED, keyNew.status)
                checkForChanges(keyNew)
                assertEquals(KeyStatus.CHECKED, keyNew.status)

                val status = query("SELECT status from keys WHERE id = ${keyNew.id}").getString("status")
                assertEquals(KeyStatus.CHECKED, KeyStatus.valueOf(status))
            }
        }

        on("getting a key that has changed") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user.id, someTime, someTime)
            val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

            it("returns true") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue2)
                assertTrue(checkForChanges(keyNew))
            }

            it("adds a new change object to the database") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue2)
                val countBefore = query("SELECT COUNT(*) FROM changes").getInt(1)
                checkForChanges(keyNew)
                val countAfter = query("SELECT COUNT(*) FROM changes").getInt(1)
                assertEquals(countBefore + 1, countAfter)
            }

            it("creates a change object correctly referencing the two keys") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue2)

                val result = query("SELECT * from keys WHERE id = ${keyNew.id}")
                assertEquals(key1.id, result.getInt("last_key"))
                assertEquals(keyNew.id, result.getInt("new_key"))
            }

            it("sets the correct status on the new change object") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue2)

                val status = query("SELECT status from keys WHERE id = ${keyNew.id}").getString("status")
                assertEquals(KeyChangeStatus.NEW, KeyChangeStatus.valueOf(status))
            }

            it("marks the key as having been checked") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                assertEquals(KeyStatus.UNCHECKED, keyNew.status)
                checkForChanges(keyNew)
                assertEquals(KeyStatus.CHECKED, keyNew.status)

                val status = query("SELECT status from keys WHERE id = ${keyNew.id}").getString("status")
                assertEquals(KeyStatus.CHECKED, KeyStatus.valueOf(status))
            }
        }

        on("getting a key that has no prior history") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user.id, someTime, someTime)

            it("treats it as if no change has been detected") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "101112")
                assertFalse(checkForChanges(keyNew))
            }

            it("doesn't save any change objects") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "101113")
                val countBefore = query("SELECT COUNT(*) FROM changes").getInt(1)
                checkForChanges(keyNew)
                val countAfter = query("SELECT COUNT(*) FROM changes").getInt(1)
                assertEquals(countBefore, countAfter)
            }


            it("marks the key as having been checked") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "101114")
                assertEquals(KeyStatus.UNCHECKED, keyNew.status)
                checkForChanges(keyNew)
                assertEquals(KeyStatus.CHECKED, keyNew.status)

                val status = query("SELECT status from keys WHERE id = ${keyNew.id}").getString("status")
                assertEquals(KeyStatus.CHECKED, KeyStatus.valueOf(status))
            }
        }
    }
})