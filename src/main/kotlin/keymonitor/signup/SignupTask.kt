package keymonitor.signup

import keymonitor.common.CONFIGS
import keymonitor.common.PhoneNumber
import keymonitor.common.sendMessage
import keymonitor.database.*
import java.io.File
import java.time.Instant

private val REGISTRATION_SUBJECT = "Welcome to Key Monitor!"
private val REGISTRATION_MESSAGE = """
Hello and welcome to Key Monitor!

We received a request from the phone number %s to notify this email address
whenever Signal Private Messenger thinks the number's owner got a new phone, or reinstalled the app
(also known as changing Safety Numbers, or keys).

In the future, when this happens, you'll get an email from us.
You can always unsubscribe by visiting the following link:
%s

If you did not subscribe to this service, please click the unsubscribe link above to avoid receiving messages in the future.
"""

/**
 * Process signups for the service
 *
 * Specifically:
 * 1. Check for any new messages signing up to the service
 * 2. Save the user and email to the database
 * 3. Send them a notification message
 */
fun run() {
    // Create temporary file for the CLI to output to
    val tempFile = File.createTempFile("signuptask.signal-cli", ".log")
    tempFile.deleteOnExit()

    // Run signal-cl and wait for it to finish
    val process = Runtime.getRuntime()?.exec(arrayOf("signal-cli", "--username",
            CONFIGS.SIGNAL_PHONE_NUMBER, "json",
            "--logfile", tempFile.absolutePath,
            "--ignore-attachments",
            "--timeout", "1"))
            ?: throw RuntimeException("failed to launch signal-cli subprocess")
    if (process.waitFor() != 0) throw RuntimeException("signal-cli halted with non-zero exit code")

    // Parse the signal-cli output for new subscription messages
    val messages = parseJsonFile(tempFile)

    // Process each message
    messages.forEach { message ->
        // Store the new user in the database
        val storedEmail = processRegistration(message)

        // Schedule a key lookup to happen ASAP
        createTask(storedEmail.userID, Instant.now())

        // Send confirmation message
        val unsubscribeLink = CONFIGS.UNSUBSCRIBE_SERVER + "unsubscribe?t=" + storedEmail.unsubscribeToken
        val text = REGISTRATION_MESSAGE.format(message.phoneNumber, unsubscribeLink)
        sendMessage(storedEmail.email, REGISTRATION_SUBJECT, text)
    }
}

/**
 * Update database with newly received registration message
 *
 * - If the user is new, store them and their email in the database.
 * - If the user already exists, invalidate their current email, and store the new one.
 * - If the user already exists and so does the email, treat the new email like it's new.
 *     This is easiest, but may not be ideal;
 *     for example, it will result in duplicate emails being sent.
 *     TODO: decide if we actually want it to behave this way
 */
internal fun processRegistration(registration: RegistrationMessage): Email {
    val (phoneNumber, email) = registration
    var user = getUser(phoneNumber)

    if (user == null) {
        // This is a new user. Save them to the database.
        user = createUser(phoneNumber)
    } else {
        // This user already exists. Do they have an active email?
        val currentEmail = getActiveEmail(user)
        if (currentEmail != null) {
            // The active email should be deactivated; it will be replaced with the new one.
            currentEmail.status = EmailStatus.REPLACED
            currentEmail.save()
        }

        // If the user is currently deactivated (because they unsubscribed), reactivate them.
        if (user.status == UserStatus.DEACTIVATED) {
            user.status = UserStatus.ACTIVE
            user.save()
        }
    }

    val newEmail = addEmail(user, email)
    return newEmail
}

data class RegistrationMessage(val phoneNumber: PhoneNumber, val email: String)