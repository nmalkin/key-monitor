package keymonitor.database

import keymonitor.common.getRandomHex
import java.sql.SQLException
import java.util.*

/** Enum representing whether the user wants to receive messages at this address. */
enum class EmailStatus { ACTIVE, REPLACED, UNSUBSCRIBED }

/** Represents an email address record in the database. */
data class Email(val id: Int,
                 val user: User,
                 val email: String,
                 val status: EmailStatus,
                 val unsubscribeToken: String)

internal val CREATE_EMAIL_TABLE =
        """CREATE TABLE IF NOT EXISTS emails (
           id INTEGER PRIMARY KEY,
           user INTEGER NOT NULL,
           email VARCHAR(128) NOT NULL,
           email_status VARCHAR(16) NOT NULL,
           unsubscribe_token VARCHAR(64) NOT NULL,

           FOREIGN KEY(user) REFERENCES users(id)
        )
    """

private val INSERT_EMAIL = "INSERT INTO emails VALUES(null, ?, ?, ?, ?)"

/**
 * Insert email into the database, associating it with given user.
 *
 * @return new Email object representing the created email
 */
fun addEmail(user: User, email: String): Email {
    // Prepare statement
    val statement = Database.connection.prepareStatement(INSERT_EMAIL)
    statement.setInt(1, user.id)
    statement.setString(2, email)
    statement.setString(3, EmailStatus.ACTIVE.name)
    val unsubscribeToken = Date().getTime().toString() + getRandomHex()
    statement.setString(4, unsubscribeToken)

    // Execute statement
    val rowsAffected = statement.executeUpdate()
    if (rowsAffected == 0) throw SQLException("failed creating email (no rows affected)")

    // Get ID of newly created email
    val keys = statement.generatedKeys
    if (!keys.next()) throw SQLException("failed creating user (cannot access created ID)")
    val id = keys.getInt(1)

    // Set up a new Email object
    return Email(id, user, email, EmailStatus.ACTIVE, unsubscribeToken)
}