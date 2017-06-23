package keymonitor.database


import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.useNewTestingDatabase
import keymonitor.database.Database.connection
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


private val someTime = Instant.EPOCH!!
private val someOtherTime = Instant.ofEpochSecond(1_000_000L)!!

class TaskTest : Spek({
    describe("a scheduled key lookup in the database") {
        beforeGroup {
            useNewTestingDatabase()
        }

        on("adding a task") {
            it("doesn't throw an exception") {
                val user = createUser(PhoneNumber("+18885550123"))
                assertNotNull(createTask(user, someTime, someOtherTime))
            }

            it("adds results to the database") {
                val countBefore = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks")
                        .getInt(1)

                val user = createUser(PhoneNumber("+18885550123"))
                createTask(user, someTime, someOtherTime)

                val countAfter = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks")
                        .getInt(1)

                assertEquals(countBefore + 1, countAfter)
            }

            it("stores the right time values") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someOtherTime)

                val result = connection.createStatement()
                        .executeQuery("SELECT * FROM lookup_tasks WHERE id = ${task.id}")
                assertTrue(result.next())
                assertEquals("1970-01-01T00:00:00Z", result.getString("not_before"))
                assertEquals("1970-01-12T13:46:40Z", result.getString("expires"))
            }
            it("gives the task the correct status") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someOtherTime)

                val status = connection.createStatement()
                        .executeQuery("SELECT status FROM lookup_tasks WHERE id = ${task.id}")
                        .getString("status")
                assertEquals(LookupTaskStatus.PENDING, LookupTaskStatus.valueOf(status))
            }

            it("references the right user") {
                val user = createUser(PhoneNumber("+18885550123"))
                val task = createTask(user, someTime, someOtherTime)

                val result = connection.createStatement()
                        .executeQuery("SELECT user_id FROM lookup_tasks WHERE id=${task.id}")
                assertTrue(result.next())
                assertEquals(user.id, result.getInt(1))
            }
        }

        on("updating a task") {
            val user = createUser(PhoneNumber("+18885550123"))
            val task = createTask(user, someTime, someOtherTime)

            it("changes the user in the database") {
                task.status = LookupTaskStatus.COMPLETED
                task.save()

                val status = connection.createStatement()
                        .executeQuery("SELECT status FROM lookup_tasks WHERE id = ${task.id}")
                        .getString("status")
                assertEquals(LookupTaskStatus.COMPLETED, LookupTaskStatus.valueOf(status))
            }
        }

        on("checking if a task is expired") {
            val user = createUser(PhoneNumber("+18885550123"))

            it("returns true if it is") {
                val expires = someOtherTime.plusSeconds(1)
                val task = createTask(user, someTime, expires)
                val now = expires.plusMillis(1)
                assertTrue(task.pastExpiration(now))
            }

            it("returns false if it isn't") {
                val expires = someOtherTime.plusSeconds(1)
                val task = createTask(user, someTime, expires)
                val now = expires.minusMillis(1)
                assertFalse(task.pastExpiration(now))
            }

            it("returns false (not expired) if it expires at this exact moment") {
                val expires = someOtherTime.plusSeconds(1)
                val task = createTask(user, someTime, expires)
                val now = expires
                assertFalse(task.pastExpiration(now))
            }
        }

        on("getting pending tasks") {
            val user = createUser(PhoneNumber("+18885550123"))

            it("returns tasks whose status is actually pending") {
                val pending = pendingTasks(Instant.EPOCH.minusSeconds(1))
                pending.forEach { task ->
                    val status = connection.createStatement()
                            .executeQuery("SELECT status FROM lookup_tasks WHERE id = ${task.id}")
                            .getString("status")
                    assertEquals(LookupTaskStatus.PENDING, LookupTaskStatus.valueOf(status))
                }
            }

            it("returns all tasks, if the cut-off is set to be sufficiently large") {
                val pending = pendingTasks(someOtherTime.plusSeconds(1_000_000L))
                val count = connection.createStatement()
                        .executeQuery("SELECT COUNT(*) FROM lookup_tasks WHERE status = '${LookupTaskStatus.PENDING.name}'")
                        .getInt(1)
                assertEquals(count, pending.size)
            }

            it("doesn't return any tasks before the cut-off") {
                // Make sure we have some tasks before and after the cut-off
                val cutoff = Instant.EPOCH.plusSeconds(60)
                val task1 = createTask(user, cutoff.minusSeconds(1), someOtherTime)
                val task2 = createTask(user, cutoff.plusSeconds(1), someOtherTime)

                val pending = pendingTasks(cutoff)

                // Check for our specific tasks
                assertTrue(pending.contains(task1))
                assertFalse(pending.contains(task2))

                // Check the remaining tasks
                pending.forEach { task ->
                    assertTrue(task.notBefore.isBefore(cutoff))
                }
            }

            it("returns tasks past their expiry time, if they haven't been marked as such") {
                val cutoff = someOtherTime.plusSeconds(123)
                val expires = cutoff.minusSeconds(1) // expired already
                val task = createTask(user, cutoff.minusSeconds(2), expires)
                // Sanity check: this task has expired
                assertTrue(task.pastExpiration(cutoff))

                val pending = pendingTasks(cutoff)
                assertTrue(pending.contains(task))
            }

            it("doesn't return tasks that have been marked as expired") {
                val task = createTask(user, someTime, someOtherTime)
                task.status = LookupTaskStatus.EXPIRED
                task.save()

                val pending = pendingTasks(Instant.EPOCH)
                assertFalse(pending.contains(task))
            }
        }

        on("getting active tasks") {
            val user = createUser(PhoneNumber("+18885550123"))

            it("doesn't return expired tasks") {
                val cutoff = someOtherTime.plusSeconds(123)
                val expires = cutoff.minusSeconds(1)
                val task = createTask(user, cutoff.minusSeconds(2), expires)

                assertTrue(task.pastExpiration(cutoff))
                assertTrue(pendingTasks(cutoff).contains(task))

                assertFalse(activeTasks(cutoff).contains(task))
            }

            it("marks expired tasks as such in the database") {
                val cutoff = someOtherTime.plusSeconds(123)
                val expires = cutoff.minusSeconds(1)
                val task = createTask(user, cutoff.minusSeconds(2), expires)

                val before = connection.createStatement()
                        .executeQuery("SELECT status FROM lookup_tasks WHERE id = ${task.id}")
                        .getString("status")
                assertEquals(LookupTaskStatus.PENDING, LookupTaskStatus.valueOf(before))

                activeTasks(cutoff)

                val after = connection.createStatement()
                        .executeQuery("SELECT status FROM lookup_tasks WHERE id = ${task.id}")
                        .getString("status")
                assertEquals(LookupTaskStatus.EXPIRED, LookupTaskStatus.valueOf(after))
            }
        }

        afterGroup {
            closeTestingDatabase()
        }
    }
})
