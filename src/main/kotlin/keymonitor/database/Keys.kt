package keymonitor.database

import java.time.Instant

val CREATE_KEY_TABLE =
        """CREATE TABLE IF NOT EXISTS keys (
            id INTEGER PRIMARY KEY,
            task_id INTEGER NOT NULL,
            user_id INTEGER NOT NULL,
            lookup_time VARCHAR(32) NOT NULL,
            lookup_phone VARCHAR(32) NOT NULL,
            lookup_ip VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL,
            value VARCHAR(256) NOT NULL,
            created_at VARCHAR(32) DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY(task_id) REFERENCES tasks(id)
            FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """

private val UPDATE_KEY = "UPDATE KEYS SET status = ? WHERE id = ?"

/** Represents whether they has been compared to previous versions for changes */
enum class KeyStatus { UNCHECKED, CHECKED }

/** Represents the results of a key lookup */
data class Key(val id: Int,
               val taskID: Int,
               val userID: Int,
               val lookupTime: Instant,
               val lookupPhone: String,
               val lookupIP: String,
               private var _status: KeyStatus,
               val value: String) {

    /** Saves changes to the key's status */
    var status: KeyStatus
        get() = _status
        set(value) {
            _status = value

            val rowsAffected = with(Database.connection.prepareStatement(UPDATE_KEY)) {
                setString(1, _status.toString())
                setInt(2, id)

                executeUpdate()
            }

            if (rowsAffected == 0) throw DataStateError("failed at updating key $id")
        }
}

private val INSERT_KEY = "INSERT INTO keys VALUES(null, ?, ?, ?, ?, ?, ?, ?, ?)"

/** Save a new key with the given values to the database */
fun saveKey(task: LookupTask, lookupTime: Instant, lookupPhone: String, lookupIP: String, value: String): Key {
    val row = with(Database.connection.prepareStatement(INSERT_KEY)) {
        setInt(1, task.id)
        setInt(2, task.userID)
        setString(3, lookupTime.toString())
        setString(4, lookupPhone)
        setString(5, lookupIP)
        setString(6, KeyStatus.UNCHECKED.name)
        setString(7, value)
        setString(8, Instant.now().toString()) // created_at

        executeUpdate()
        generatedKeys
    }

    if (!row.next()) throw DataStateError("failed creating user (cannot access created ID)")
    val id = row.getInt(1)

    return Key(id,
            taskID = task.id,
            userID = task.userID,
            lookupTime = lookupTime,
            lookupPhone = lookupPhone,
            lookupIP = lookupIP,
            _status = KeyStatus.UNCHECKED,
            value = value)
}
