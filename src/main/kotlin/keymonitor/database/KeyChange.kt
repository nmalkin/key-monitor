package keymonitor.database

import java.time.Instant

val CREATE_CHANGE_TABLE =
        """CREATE TABLE IF NOT EXISTS changes (
            id INTEGER PRIMARY KEY,
            user_id INTEGER NOT NULL,
            last_key INTEGER NOT NULL,
            new_key INTEGER NOT NULL,
            status VARCHAR(32) NOT NULL,
            created_at VARCHAR(32) DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY(user_id) REFERENCES tasks(id)
            FOREIGN KEY(last_key) REFERENCES keys(id)
            FOREIGN KEY(new_key) REFERENCES keys(id)
            )
        """

private val UPDATE_CHANGE = "UPDATE changes SET status = ? WHERE id = ?"

enum class KeyChangeStatus { NEW, NOTIFIED }

/** Represents a detected change in a user's keys */
data class KeyChange(val id: Int,
                     val userID: Int,
                     val lastKeyID: Int,
                     val newKeyID: Int,
                     private var _status: KeyChangeStatus) {

    var status: KeyChangeStatus
        get() = _status
        /** Saves changes to the status */
        set(value) {
            _status = value

            val rowsAffected = with(Database.connection.prepareStatement(UPDATE_CHANGE)) {
                setString(1, _status.toString())
                setInt(2, id)

                executeUpdate()
            }

            if (rowsAffected == 0) throw DataStateError("failed at updating $id")
        }
}

private val INSERT_CHANGE = "INSERT INTO changes VALUES (null, ?, ?, ?, ?, ?)"

fun saveChange(userID: Int, lastKeyID: Int, newKeyID: Int): KeyChange {
    val row = with(Database.connection.prepareStatement(INSERT_CHANGE)) {
        setInt(1, userID)
        setInt(2, lastKeyID)
        setInt(3, newKeyID)
        setString(4, KeyChangeStatus.NEW.name)
        setString(5, Instant.now().toString()) // created_at

        executeUpdate()
        generatedKeys
    }

    if (!row.next()) throw DataStateError("failed creating key change (cannot access created ID)")
    val id = row.getInt(1)

    return KeyChange(id = id, userID = userID, lastKeyID = lastKeyID, newKeyID = newKeyID, _status = KeyChangeStatus.NEW)
}

private val SELECT_ALL_NEW = "SELECT * FROM changes where STATUS = '${KeyChangeStatus.NEW.name}'"

/**
 * Return all key changes with the status of KeyChangeStatus.NEW
 */
fun getAllNewChanges(): Collection<KeyChange> {
    val result = Database.connection.prepareStatement(SELECT_ALL_NEW).executeQuery()

    val changes = mutableListOf<KeyChange>()
    while (result.next()) {
        val change = KeyChange(
                id = result.getInt("id"),
                userID = result.getInt("user_id"),
                lastKeyID = result.getInt("last_key"),
                newKeyID = result.getInt("new_key"),
                _status = KeyChangeStatus.valueOf(result.getString("status")))
        changes.add(change)
    }

    return changes
}
