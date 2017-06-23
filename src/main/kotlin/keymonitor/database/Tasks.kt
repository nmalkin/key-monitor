package keymonitor.database

import java.sql.SQLException
import java.time.Instant

/** Defines the status of this task */
enum class LookupTaskStatus { PENDING, COMPLETED, EXPIRED }

/** Represents a scheduled key lookup, backed by a database object */
data class LookupTask(val id: Int,
                      val userID: Int,
                      val notBefore: Instant,
                      val expires: Instant,
                      var status: LookupTaskStatus)

internal val CREATE_TASK_TABLE =
        """CREATE TABLE IF NOT EXISTS lookup_tasks (
            id INTEGER PRIMARY KEY,
            user_id INTEGER NOT NULL,
            not_before VARCHAR(32) NOT NULL,
            expires VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL,
            created_at VARCHAR(32) DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """

private val INSERT_TASK = "INSERT INTO lookup_tasks VALUES(null, ?, ?, ?, ?, ?)"

/** Create a task in the database with the provided parameters */
fun createTask(user: User, notBefore: Instant, expires: Instant): LookupTask {
    val keys = with(Database.connection.prepareStatement(INSERT_TASK)) {
        setInt(1, user.id)
        setString(2, notBefore.toString())
        setString(3, expires.toString())
        setString(4, LookupTaskStatus.PENDING.name)
        setString(5, Instant.now().toString()) // created_at

        executeUpdate()
        generatedKeys
    }

    if (!keys.next()) throw SQLException("failed creating user (cannot access created ID)")
    val id = keys.getInt(1)

    return LookupTask(id, user.id, notBefore, expires, LookupTaskStatus.PENDING)
}

private val UPDATE_TASK = "UPDATE lookup_tasks SET status = ? WHERE id = ?"

/** Save changes to the given task's status to the database */
fun LookupTask.save() {
    val affected: Int = with(Database.connection.prepareStatement(UPDATE_TASK)) {
        setString(1, status.toString())
        setInt(2, id)

        executeUpdate()
    }

    if (affected == 0) throw SQLException("failed at updating task $id")
}

/**
 * Returns true if the expiration time of this task has passed
 *
 * Important: this is independent of whether or not its status is LookupTaskStatus.EXPIRED
 */
fun LookupTask.pastExpiration(now: Instant = Instant.now()): Boolean {
    return now.isAfter(expires)
}

private val SELECT_PENDING_TASKS = """SELECT * FROM lookup_tasks
                                      WHERE status = '${LookupTaskStatus.PENDING.name}'
                                      AND not_before < ?
                                   """

/**
 * Get all tasks pending in the database, with their notBefore set to prior to the given cut-off
 *
 * For example, if it's 12:05, we we want all (pending) from 12:04 and earlier, but not anything after then.
 */
internal fun pendingTasks(cutoff: Instant = Instant.now()): Collection<LookupTask> {
    val result = with(Database.connection.prepareStatement(SELECT_PENDING_TASKS)) {
        setString(1, cutoff.toString())

        executeQuery()
    }

    val tasks = mutableSetOf<LookupTask>()
    while (result.next()) {
        val taskID = result.getInt("id")

        val task = LookupTask(taskID,
                userID = result.getInt("user_id"),
                notBefore = Instant.parse(result.getString("not_before")),
                expires = Instant.parse(result.getString("expires")),
                status = LookupTaskStatus.valueOf(result.getString("status")))
        tasks.add(task)
    }

    return tasks
}
