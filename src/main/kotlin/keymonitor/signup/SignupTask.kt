package keymonitor.signup

import keymonitor.PhoneNumber
import keymonitor.database.createUser
import java.io.File

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

    val messages = parseJsonFile(tempFile)

    for (message in messages) {
        val user = createUser(message.phoneNumber)
    }
}

data class RegistrationMessage(val phoneNumber: PhoneNumber, val email: String)


