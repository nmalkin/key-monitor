package keymonitor.signup

import keymonitor.common.CONFIGS
import keymonitor.common.PhoneNumber
import keymonitor.common.logger
import keymonitor.common.sendMessage
import keymonitor.database.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import java.util.logging.Logger

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
    val serverNumber = CONFIGS.SIGNAL_PHONE_NUMBER
    logger.info("using receiver phone number $serverNumber")

    // Run signal-cli and wait for it to finish
    logger.info("receiving messages via signal-cli")
    val process = Runtime.getRuntime()?.exec(arrayOf("signal-cli", "--username",
            serverNumber,
            "receive", "--json",
            "--ignore-attachments",
            "--timeout", "1"))
            ?: throw RuntimeException("failed to launch signal-cli subprocess")
    if (process.waitFor() != 0) throw RuntimeException("signal-cli halted with non-zero exit code")

    // Capture the cli's output
    var output = BufferedReader(InputStreamReader(process.inputStream))

    // Parse the signal-cli output for new subscription messages
    logger.info("parsing received messages")
    val messages = parseJson(output)

    // Process each message
    messages.forEach { message ->
        logger.info("processing registration message $message")

        // Store the new user in the database
        val storedEmail = processRegistration(message)

        // Schedule a key lookup to happen ASAP
        createTask(storedEmail.userID, Instant.now())

        // Send confirmation message
        // TODO: this will send a confirmation message even if the email is already in the system
        // This could be good (in case they forgot they signed up) or bad (if we get duplicate messages).
        val unsubscribeLink = CONFIGS.UNSUBSCRIBE_SERVER + "?t=" + storedEmail.unsubscribeToken
        val text = REGISTRATION_MESSAGE.format(message.phoneNumber, unsubscribeLink)
        sendMessage(storedEmail.email, REGISTRATION_SUBJECT, text)
    }
}

/**
 * Update database with newly received registration message
 *
 * - If the user is new, store them and their email in the database.
 * - If the user already exists, store the email, and associate it with the current user.
 * - If the user already exists and so does the email, don't store the email.
 */
internal fun processRegistration(registration: RegistrationMessage): Email {
    logger.info("processing registration $registration")

    val (phoneNumber, email) = registration
    var user = getUser(phoneNumber)

    if (user == null) {
        // This is a new user. Save them to the database.
        logger.info("This is a new user. Save them to the database.")

        user = createUser(phoneNumber)
    } else { // This user already exists.
        logger.info("This user already exists.")

        // If the user is currently deactivated (because they unsubscribed), reactivate them.
        if (user.status == UserStatus.DEACTIVATED) {
            user.status = UserStatus.ACTIVE
            user.save()
        }

        // Is this email already stored?
        getUserEmails(user.id).forEach { existingEmail ->
            if (existingEmail.email == email) {
                logger.info("email $email is already registered")

                // The requested email already exists in our database and is active.
                // We don't need to add a new one.
                return existingEmail
            }
        }

        // TODO: it may be good to notify existing active email(s) if a new one is added so that a user is always aware of the addresses associated with their account
    }

    val newEmail = addEmail(user, email)
    return newEmail
}

data class RegistrationMessage(val phoneNumber: PhoneNumber, val email: String)
