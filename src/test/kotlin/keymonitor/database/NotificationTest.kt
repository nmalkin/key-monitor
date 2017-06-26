package keymonitor.database

import keymonitor.common.PhoneNumber
import keymonitor.common.closeTestingDatabase
import keymonitor.common.query
import keymonitor.common.useNewTestingDatabase
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant
import kotlin.test.assertEquals


class NotificationTest : Spek({
    describe("a notification") {
        beforeGroup { useNewTestingDatabase() }

        on("saving an email notification") {
            val phone = "+15105550123"
            val user = createUser(PhoneNumber(phone))
            val email = addEmail(user, "test@example.com")
            val time = Instant.now()
            val task = createTask(user.id, time, time)
            val key1 = saveKey(task, time, phone, "127.0.0.1", "010203")
            val key2 = saveKey(task, time, phone, "127.0.0.1", "010204")
            val change = saveChange(user.id, key1.id, key2.id)

            it("adds a row to the database") {
                val countBefore = query("SELECT COUNT(*) FROM notifications").getInt(1)
                saveNotificationEmail(user.id, change.id, email.id, time)
                val countAfter = query("SELECT COUNT(*) FROM notifications").getInt(1)
                assertEquals(countBefore + 1, countAfter)
            }

            it("saves the right values") {
                val notification = saveNotificationEmail(user.id, change.id, email.id, time)

                val result = query("SELECT * FROM notifications WHERE id = ${notification.id}")
                assertEquals(user.id, result.getInt("user_id"))
                assertEquals(change.id, result.getInt("change_id"))
                assertEquals(email.id, result.getInt("email_id"))
                assertEquals(time, Instant.parse(result.getString("sent")))
            }

            it("automatically notes that this is an email notification") {
                val notification = saveNotificationEmail(user.id, change.id, email.id, time)

                val result = query("SELECT * FROM notifications WHERE id = ${notification.id}")
                assertEquals(NotificationType.EMAIL, NotificationType.valueOf(result.getString("notification_type")))
            }

            it("returns an object with the expected values") {
                val notification = saveNotificationEmail(user.id, change.id, email.id, time)

                val expected = Notification(notification.id, user.id, change.id, NotificationType.EMAIL, email.id, time)

                assertEquals(expected, notification)
            }
        }

        afterGroup { closeTestingDatabase() }
    }
})