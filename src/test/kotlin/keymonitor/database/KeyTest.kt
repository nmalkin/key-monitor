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
import kotlin.test.*


private val phoneNumber = PhoneNumber("+18885550123")
private val someTime = Instant.ofEpochSecond(1_000_000L)!!
private val ip = "127.0.0.1"
private val keyValue = "010203"

class KeyTest : Spek({
    describe("keys saved in the database") {
        beforeGroup {
            useNewTestingDatabase()
        }

        on("saving a new key") {
            it("adds results to the database") {
                val countBefore = query("SELECT COUNT(*) FROM keys").getInt(1)

                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                val countAfter = query("SELECT COUNT(*) FROM keys").getInt(1)

                assertEquals(countBefore + 1, countAfter)
            }

            it("stores the right values") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                val result = query("SELECT * FROM keys WHERE id = ${key.id}")
                assertTrue(result.next())

                assertEquals(user.id, result.getInt("user_id"))
                assertEquals(task.id, result.getInt("task_id"))
                assertEquals(ip, result.getString("lookup_ip"))
                assertEquals(phoneNumber.toString(), result.getString("lookup_phone"))
                assertEquals(keyValue, result.getString("value"))
            }

            it("gives the key the correct status") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                val status = query("SELECT status FROM keys WHERE id = ${key.id}").getString("status")
                assertEquals(KeyStatus.UNCHECKED, KeyStatus.valueOf(status))
            }
        }

        on("updating a key") {
            it("changes the user in the database automatically") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                key.status = KeyStatus.CHECKED

                val status = query("SELECT status FROM keys WHERE id = ${task.id}").getString("status")
                assertEquals(KeyStatus.CHECKED, KeyStatus.valueOf(status))
            }
        }

        on("selecting a specific key") {
            it("returns the right key") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                assertEquals(key, getKey(key.id))
            }

            it("throws an exception if called with an invalid key") {
                assertFailsWith<IllegalArgumentException> {
                    getKey(-999)
                }
            }
        }

        on("selecting the last key") {
            it("returns the right key") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                key1.status = KeyStatus.CHECKED
                saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                assertEquals(key1.id, getLastCheckedKey(user.id)?.id)
            }

            it("returns a key with the right values") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                key.status = KeyStatus.CHECKED

                val lastKey = getLastCheckedKey(user.id)

                assertEquals(key, lastKey)
            }

            it("selects the last one when multiple are available") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                key1.status = KeyStatus.CHECKED
                val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
                key2.status = KeyStatus.CHECKED

                assertEquals(key2, getLastCheckedKey(user.id))
            }

            it("returns null if no key was found") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                saveKey(task, someTime, phoneNumber.toString(), ip, keyValue) // key is UNCHECKED

                assertNull(getLastCheckedKey(user.id))
            }
        }

        on("selecting all matching key") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user, someTime, someTime)
            val key1 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            val key2 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)
            key2.status = KeyStatus.CHECKED
            val key3 = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

            val newKeys = getAll(KeyStatus.UNCHECKED)

            it("returns the right keys") {
                assertTrue(newKeys.contains(key1))
                assertTrue(newKeys.contains(key3))
            }

            it("doesn't return the wrong keys") {
                assertFalse(newKeys.contains(key2))
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})
