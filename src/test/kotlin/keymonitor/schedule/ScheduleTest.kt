package keymonitor.schedule

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.Database.connection
import keymonitor.database.createUser
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleTest : Spek({
    describe("scheduling") {
        beforeGroup {
            useNewTestingDatabase()
        }

        on("on scheduling a lookup for a specific user") {
            val user = createUser(PhoneNumber("+18885550123"))

            it("saves the new task to the database") {
                val countBefore = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks")
                        .getInt(1)

                scheduleTaskForUser(user, Instant.EPOCH)

                val countAfter = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks")
                        .getInt(1)

                assertEquals(countBefore + 1, countAfter)
            }

            it("references the right user") {
                val task = scheduleTaskForUser(user, Instant.EPOCH)
                val userID = connection.createStatement()
                        .executeQuery("SELECT * FROM lookup_tasks WHERE id = ${task.id}")
                        .getInt("user_id")
                assertEquals(user.id, userID)
            }

            it("schedules the task during the correct interval") {
                val startTime = Instant.EPOCH
                for (i in 1..1000) {
                    // Repeat so we get a bunch of different scheduled times
                    val task = scheduleTaskForUser(user, startTime)
                    assertTrue(task.notBefore.isAfter(startTime))
                    assertTrue(task.expires.isBefore(startTime.plus(LOOKUP_FREQUENCY)))
                }
            }

            it("schedules the tasks at different points in the interval") {
                for (i in 1..10000) {
                    scheduleTaskForUser(user, Instant.EPOCH)
                }

                val result = connection.createStatement()
                        .executeQuery("SELECT notBefore FROM lookup_tasks")
                val times: MutableSet<String> = hashSetOf()
                while (result.next()) {
                    times.add(result.getString(1))
                }

                // They haven't all been scheduled at the same time
                assertTrue(times.distinct().size > 1)

                // Ensure that something got scheduled at each potential time
                assertEquals(LOOKUP_FREQUENCY.toMinutes().toInt(), times.distinct().size)
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})
