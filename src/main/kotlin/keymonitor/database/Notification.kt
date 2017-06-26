package keymonitor.database

import java.time.Instant

val CREATE_NOTIFICATION_TABLE =
        """CREATE TABLE IF NOT EXISTS notifications (
            id INTEGER PRIMARY KEY,
            user_id INTEGER NOT NULL,
            change_id INTEGER NOT NULL,
            notification_type VARCHAR(32) NOT NULL,
            email_id INTEGER,
            sent VARCHAR(32) NOT NULL,
            created_at VARCHAR(32) DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY(user_id) REFERENCES tasks(id)
            FOREIGN KEY(change_id) REFERENCES changes(id)
            )
        """

enum class NotificationType { EMAIL }

/** Represents a detected Notification in a user's keys */
data class Notification(val id: Int,
                        val userID: Int,
                        val changeID: Int,
                        val type: NotificationType,
                        val email: Int?,
                        val sent: Instant)

private val INSERT_NOTIFICATION = "INSERT INTO notifications VALUES (null, ?, ?, ?, ?, ?, ?)"

fun saveNotificationEmail(userID: Int, changeID: Int, emailID: Int, sent: Instant): Notification {
    val row = with(Database.connection.prepareStatement(INSERT_NOTIFICATION)) {
        setInt(1, userID)
        setInt(2, changeID)
        setString(3, NotificationType.EMAIL.name)
        setInt(4, emailID)
        setString(5, sent.toString())
        setString(6, Instant.now().toString()) // created_at

        executeUpdate()
        generatedKeys
    }

    if (!row.next()) throw DataStateError("failed saving notification (cannot access created ID)")
    val id = row.getInt(1)

    return Notification(id,
            userID = userID,
            changeID = changeID,
            type = NotificationType.EMAIL,
            email = emailID,
            sent = sent)
}
