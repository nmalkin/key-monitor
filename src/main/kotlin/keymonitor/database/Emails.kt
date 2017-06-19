package keymonitor.database

import keymonitor.common.getRandomHex
import java.sql.SQLException
import java.util.*

/** Enum representing whether the user wants to receive messages at this address. */
enum class EmailStatus { ACTIVE, REPLACED, UNSUBSCRIBED }

/**
 * Represents an email address record in the database
 *
 * The `status` of the address can be changed; all other fields are immutable.
 */
data class Email(val id: Int,
                 val user: User,
                 val email: String,
                 var status: EmailStatus,
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

private val GET_EMAIL = "SELECT * FROM emails WHERE user = ? AND email_status = '${EmailStatus.ACTIVE.name}'"

/**
 * Look up and return the given user's *active* email
 *
 * An active email is one with a status of EmailStatus.ACTIVE
 *
 * A user can have zero active emails (for example, if they unsubscribed), or one active one.
 * There should never be more than one. (If there is, an exception is thrown.)
 *
 * @param user the user to look up
 * @return the user's active email, or null if it doesn't exist
 * @throws RuntimeException if there are multiple active emails (this shouldn't happen)
 */
fun getActiveEmail(user: User): Email? {
    val statement = Database.connection.prepareStatement(GET_EMAIL)
    statement.setInt(1, user.id)

    val result = statement.executeQuery()
    if (!result.next()) return null

    val email = Email(result.getInt("id"),
            user,
            result.getString("email"),
            EmailStatus.ACTIVE,
            result.getString("unsubscribe_token"))

    if (result.next()) throw RuntimeException("more than one active email found for user ${user.id}")

    return email
}

private val UPDATE_EMAIL = "UPDATE emails SET email_status = ? WHERE id = ?"

/**
 * Saves the current values of the Email object to the database
 *
 * As `status` is currently the only mutable field, it's the only one that is updated.
 *
 * @throws RuntimeException if the row to update is missing
 */
fun Email.save() {
    val statement = Database.connection.prepareStatement(UPDATE_EMAIL)
    statement.setString(1, this.status.name)
    statement.setInt(2, this.id)

    val rowsAffected = statement.executeUpdate()
    if (rowsAffected != 1) throw RuntimeException("couldn't find the email I was supposed to update")
}