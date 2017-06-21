package keymonitor.database

import java.time.Instant

/** Represents a scheduled key lookup, backed by a database object */
data class LookupTask(val id: Int,
                      val user: User,
                      val notBefore: Instant,
                      val expires: Instant)

internal val CREATE_TASK_TABLE =
        """CREATE TABLE IF NOT EXISTS lookup_tasks (
            id INTEGER PRIMARY KEY,
            user_id INTEGER NOT NULL,
            not_before VARCHAR(32) NOT NULL,
            expires VARCHAR(32) NOT NULL,
            created_at VARCHAR(32) DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """

private val INSERT_TASK = "INSERT INTO lookup_tasks VALUES(null, ?, ?, ?, ?)"

/** Create a task in the database with the provided parameters */
fun createTask(user: User, notBefore: Instant, expires: Instant): LookupTask {
    throw NotImplementedError()
}