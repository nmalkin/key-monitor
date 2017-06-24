package keymonitor.lookup

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.query
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.KeyStatus
import keymonitor.database.LookupTaskStatus
import keymonitor.database.createTask
import keymonitor.database.createUser
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals

val testNumber = PhoneNumber("+15105550123")
private val someTime = Instant.ofEpochSecond(2_000_000L)!!
private val someOtherTime = Instant.ofEpochSecond(3_000_000L)!!

class LookupTest : Spek({
    describe("performing a lookup task") {
        val api = mock<SignalAPI> {
            on { lookup(testNumber) } doReturn setOf(byteArrayOf(1, 2, 3), byteArrayOf(4, 5, 6))
            on { user } doReturn "+18885550100"
        }

        beforeGroup {
            useNewTestingDatabase()
        }

        on("looking up a existing key") {
            it("saves the result to the database") {
                val user = createUser(testNumber)
                val task = createTask(user, someTime, someOtherTime)

                val before = query("SELECT COUNT(*) FROM keys").getInt(1)
                performLookup(task, api)
                val after = query("SELECT COUNT(*) FROM keys").getInt(1)

                assertEquals(before + 1, after)
            }

            it("saves and returns the same values") {
                val user = createUser(testNumber)
                val task = createTask(user, someTime, someOtherTime)
                val key = performLookup(task, api)

                val result = query("SELECT * FROM keys WHERE id = ${key.id}")
                assertEquals(key.lookupTime, Instant.parse(result.getString("lookup_time")))
                assertEquals(key.lookupPhone, result.getString("lookup_phone"))
                assertEquals(key.lookupIP, result.getString("lookup_ip"))
                assertEquals(key.value, result.getString("value"))
            }

            it("sets the status of the new keys as unchecked") {
                val user = createUser(testNumber)
                val task = createTask(user, someTime, someOtherTime)

                val key = performLookup(task, api)
                assertEquals(KeyStatus.UNCHECKED, key.status)

                val status = query("SELECT status FROM keys WHERE id = ${key.id}").getString("status")
                assertEquals(KeyStatus.UNCHECKED, KeyStatus.valueOf(status))
            }

            it("marks the lookup task as completed") {
                val user = createUser(testNumber)
                val task = createTask(user, someTime, someOtherTime)

                val before = query("SELECT * FROM lookup_tasks WHERE id = ${task.id}").getString("status")
                assertEquals(LookupTaskStatus.PENDING, LookupTaskStatus.valueOf(before))

                performLookup(task, api)

                val after = query("SELECT * FROM lookup_tasks WHERE id = ${task.id}").getString("status")
                assertEquals(LookupTaskStatus.COMPLETED, LookupTaskStatus.valueOf(after))
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})