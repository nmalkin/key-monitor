package keymonitor.signup

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import keymonitor.PhoneNumber
import keymonitor.PhoneNumberBuilder
import keymonitor.PhoneNumberValidator
import java.io.File

class Args {
    @Parameter(names = arrayOf("--number"), required = true,
            validateWith = arrayOf(PhoneNumberValidator::class),
            converter = PhoneNumberBuilder::class,
            description = "Phone number of the server")
    var serverPhoneNumber: PhoneNumber? = null
}

fun main(args: Array<String>) {
    // Parse program arguments
    val arguments = Args()
    JCommander.newBuilder().addObject(arguments).build().parse(*args)
    val serverNumber = arguments.serverPhoneNumber ?: throw IllegalArgumentException()

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
}

data class RegistrationMessage(val phoneNumber: PhoneNumber, val email: String)


