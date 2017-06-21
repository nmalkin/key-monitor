package keymonitor.database


import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useTestingDatabase
import keymonitor.database.Database.connection
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


val someTime = Instant.EPOCH!! // 1970-01-01T00:00:00Z
val someOtherTime = Instant.ofEpochSecond(1_000_000L)!!

class TaskTest : Spek({
    describe("a scheduled key lookup in the database") {
        var tempDB = useTestingDatabase()
        val user = createUser(PhoneNumber("+18885550123"))

        on("adding a task") {
            it("doesn't throw an exception") {
                assertNotNull(createTask(user, someTime, someOtherTime))
            }

            it("adds results to the database") {
                val countBefore = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks")
                        .getInt(1)

                createTask(user, someTime, someOtherTime)

                val countAfter = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks")
                        .getInt(1)

                assertEquals(countBefore + 1, countAfter)
            }

            it("stores the right time values") {
                val task = createTask(user, someTime, someOtherTime)

                val result = connection.createStatement()
                        .executeQuery("SELECT * FROM lookup_tasks WHERE id = ${task.id}")
                assertTrue(result.next())
                assertEquals("1970-01-01T00:00:00Z", result.getString("not_before"))
            }

            it("references the right user") {
                val task = createTask(user, someTime, someOtherTime)

                val result = connection.createStatement()
                        .executeQuery("SELECT user_id FROM lookup_tasks WHERE id=${task.id}")
                assertTrue(result.next())
                assertEquals(user.id, result.getInt(1))
            }
        }

        afterGroup {
            closeTestingDatabase(tempDB)
        }
    }
})
