package keymonitor.database

import java.sql.SQLException
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
    val keys = with(Database.connection.prepareStatement(INSERT_TASK)) {
        setInt(1, user.id)
        setString(2, notBefore.toString())
        setString(3, expires.toString())
        setString(4, Instant.now().toString()) // created_at

        executeUpdate()
        generatedKeys
    }

    if (!keys.next()) throw SQLException("failed creating user (cannot access created ID)")
    val id = keys.getInt(1)

    return LookupTask(id, user, notBefore, expires)
}