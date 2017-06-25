package keymonitor.change

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.query
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.*
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.*


private val phoneNumber = PhoneNumber("+18885550123")
private val someTime = Instant.ofEpochSecond(1_000_000L)!!
private val ip = "127.0.0.1"
private val keyValue = "010203"

class DetectChangesTest : Spek({
    beforeGroup { useNewTestingDatabase() }

    describe("checking if keys changed") {
        on("getting a key that hasn't changed") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user.id, someTime, someTime)
            val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            key1.status = KeyStatus.CHECKED

            it("returns false") {
                val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                assertNull(checkForChanges(key2))
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
            key1.status = KeyStatus.CHECKED

            it("detects the change") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "010201")
                assertNotNull(checkForChanges(keyNew))
            }

            it("adds a new change object to the database") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "010204")
                val countBefore = query("SELECT COUNT(*) FROM changes").getInt(1)
                checkForChanges(keyNew)
                val countAfter = query("SELECT COUNT(*) FROM changes").getInt(1)
                assertEquals(countBefore + 1, countAfter)
            }

            it("creates a change object correctly referencing the two keys") {
                val keyOld = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                keyOld.status = KeyStatus.CHECKED
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "010205")

                val result1 = query("SELECT * from changes WHERE new_key = ${keyNew.id}")
                assertFalse(result1.next())

                val change = checkForChanges(keyNew) ?: throw AssertionError("expected change to be detected")

                val result2 = query("SELECT * from changes WHERE id = ${change.id}")
                assertTrue(result2.next())
                assertEquals(keyOld.id, result2.getInt("last_key"))
                assertEquals(keyNew.id, result2.getInt("new_key"))
            }

            it("sets the correct status on the new change object") {
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, "010206")
                val change = checkForChanges(keyNew) ?: throw AssertionError("expected change to be detected")

                val status = query("SELECT status from changes WHERE id = ${change.id}").getString("status")
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
            it("treats it as if no change has been detected") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user.id, someTime, someTime)
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                assertNull(checkForChanges(keyNew))
            }

            it("doesn't save any change objects") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user.id, someTime, someTime)
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                val countBefore = query("SELECT COUNT(*) FROM changes").getInt(1)
                checkForChanges(keyNew)
                val countAfter = query("SELECT COUNT(*) FROM changes").getInt(1)
                assertEquals(countBefore, countAfter)
            }


            it("marks the key as having been checked") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user.id, someTime, someTime)
                val keyNew = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                assertEquals(KeyStatus.UNCHECKED, keyNew.status)
                checkForChanges(keyNew)
                assertEquals(KeyStatus.CHECKED, keyNew.status)

                val status = query("SELECT status from keys WHERE id = ${keyNew.id}").getString("status")
                assertEquals(KeyStatus.CHECKED, KeyStatus.valueOf(status))
            }
        }
    }

    afterGroup { closeTestingDatabase() }
})