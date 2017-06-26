package keymonitor.notify

import keymonitor.common.sendMessage
import keymonitor.database.*
import java.time.Instant

private val SUBJECT = "Warning: your Signal keys have changed"
private val MESSAGE = """
Hi there!

We've noticed that the keys for your phone number %s changed between %s and %s.

If you didn't get a new phone or reinstall your app during this time period, you may want to look into that.

Cheers,


Your friends at Key Monitor
"""


/**
 * Send a notification email to all active emails associated with the changed account
 */
fun notify(change: KeyChange): Collection<Notification> {
    val userID = change.userID

    val user = getUser(userID)
            ?: throw DataStateError("can't find user associated with change ${change.id}")

    val phoneNumber = user.phoneNumber
    val keyOld = getKey(change.lastKeyID)
    val keyNew = getKey(change.newKeyID)
    val message = MESSAGE.format(phoneNumber, keyOld.lookupTime, keyNew.lookupTime)

    val notifications = mutableListOf<Notification>()
    val emails = getUserEmails(change.userID)
    emails.forEach { email ->
        sendMessage(email.email, SUBJECT, message)

        val sent = Instant.now()
        val notification = saveNotificationEmail(userID = change.userID, changeID = change.id, emailID = email.id, sent = sent)
        notifications.add(notification)
    }

    change.status = KeyChangeStatus.NOTIFIED

    return notifications
}

fun run() {
    getAllNewChanges().forEach { notify(it) }
}