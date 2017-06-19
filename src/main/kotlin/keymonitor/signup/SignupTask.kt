package keymonitor.signup

import keymonitor.common.CONFIGS
import keymonitor.common.PhoneNumber
import keymonitor.common.sendMessage
import keymonitor.database.Email
import keymonitor.database.addEmail
import keymonitor.database.createUser
import java.io.File

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
fun run(serverNumber: PhoneNumber) {
    // Create temporary file for the CLI to output to
    val tempFile = File.createTempFile("signuptask.signal-cli", ".log")
    tempFile.deleteOnExit()

    // Run signal-cl and wait for it to finish
    val process = Runtime.getRuntime()?.exec(arrayOf("signal-cli", "--username",
            serverNumber.toString(), "json",
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

        // Send confirmation message
        val unsubscribeLink = CONFIGS.UNSUBSCRIBE_SERVER + "unsubscribe?t=" + storedEmail.unsubscribeToken
        val text = REGISTRATION_MESSAGE.format(message.phoneNumber, unsubscribeLink)
        sendMessage(storedEmail.email, REGISTRATION_SUBJECT, text)
    }
}

/**
 * Update database with newly received registration message
 *
 * Currently, this means storing the new user and their email in the database.
 * TODO: what if the number already exists?
 */
internal fun processRegistration(registration: RegistrationMessage): Email {
    val (phoneNumber, email) = registration
    val user = createUser(phoneNumber)
    val storedEmail = addEmail(user, email)
    return storedEmail
}

data class RegistrationMessage(val phoneNumber: PhoneNumber, val email: String)