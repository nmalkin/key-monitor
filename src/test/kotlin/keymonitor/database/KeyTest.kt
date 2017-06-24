package keymonitor.database


import keymonitor.common.PhoneNumber
import keymonitor.common.query
import keymonitor.common.useNewTestingDatabase
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue


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

                val countAfter = query("SELECT COUNT(*) FROM lookup_tasks").getInt(1)

                assertEquals(countBefore + 1, countAfter)
            }

            it("stores the right values") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                val result = query("SELECT * FROM keys WHERE id = ${key.id}")
                assertTrue(result.next())

                assertEquals(ip, result.getString("lookup_ip"))
                assertEquals(phoneNumber.toString(), result.getString("lookup_phone"))
                assertEquals(keyValue, result.getString("value"))
            }

            it("gives the key the correct status") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                val status = query("SELECT status FROM keys WHERE id = ${key.id}").getString("status")
                assertEquals(KeyStatus.NEW, KeyStatus.valueOf(status))
            }
        }

        on("updating a key") {
            it("changes the user in the database automatically") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someTime)
                val key = saveKey(task, someTime, phoneNumber.toString(), ip, keyValue)

                key.status = KeyStatus.NO_CHANGE

                val status = query("SELECT status FROM keys WHERE id = ${task.id}").getString("status")
                assertEquals(KeyStatus.NO_CHANGE, KeyStatus.valueOf(status))
            }
        }
    }
})
