package keymonitor.database

import keymonitor.PhoneNumber
import java.sql.SQLException

/** Enum representing whether the user's keys should be monitored */
enum class UserStatus {
    ACTIVE,
    DEACTIVATED
}

/** Represents a user as one exists in the database */
data class User(val id: Int, val phoneNumber: PhoneNumber, val status: UserStatus)

internal val CREATE_USER_TABLE =
        """CREATE TABLE IF NOT EXISTS users (
           id INTEGER PRIMARY KEY,
           phone VARCHAR(16) NOT NULL,
           account_status VARCHAR(16)
          )
        """

private val INSERT_USER = "INSERT INTO users VALUES (null, ?, ?)"

/**
 * Create a new user with the given phone number in the database
 *
 * @return new User object representing the created user, if the operation was successful
 * @throws SQLException if anything goes wrong
 */
fun createUser(number: PhoneNumber): User {
    // Prepare statement
    val statement = Database.connection.prepareStatement(INSERT_USER)
    statement.setString(1, number.toString())
    statement.setString(2, UserStatus.ACTIVE.name)

    // Execute statement
    val rowsAffected = statement.executeUpdate()
    if (rowsAffected == 0) throw SQLException("failed creating user (no rows affected)")

    // Get ID of newly created user
    val keys = statement.generatedKeys
    if (!keys.next()) throw SQLException("failed creating user (cannot access created ID)")
    val id = keys.getInt(1)

    // Set up a new User object
    return User(id, number, UserStatus.ACTIVE)
}
