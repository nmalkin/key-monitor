package keymonitor.signup

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import keymonitor.PhoneNumber
import keymonitor.PhoneNumberBuilder
import keymonitor.PhoneNumberValidator
import java.io.*
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonNode
import org.apache.commons.validator.routines.EmailValidator
import java.util.stream.Collectors


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

class JsonParsingException(line: String, message: String) : RuntimeException("$message in $line")

fun parseJsonFile(file: File): Collection<RegistrationMessage> {
    // Prepare to parse output
    val factory = JsonFactory()
    val emailValidator = EmailValidator.getInstance()
    fun parse(line: String): RegistrationMessage? {
        try {
            return parseLine(factory, emailValidator, line)
        } catch (e: JsonParsingException) {
            // Notify about any errors, but don't halt
            System.err.println(e)
            return null
        }
    }

    // Parse the file, filtering out any lines that don't return anything
    val reader = BufferedReader(FileReader(file))
    val messages: MutableCollection<RegistrationMessage> = reader.lines()
            .map(::parse)
            .filter { it != null }
            .collect(Collectors.toSet())

    return messages
}

fun parseLine(factory: JsonFactory, emailValidator: EmailValidator, line: String): RegistrationMessage? {
    // Parse each line separately, because log file format is one JSON entry per line
    val tree: JsonNode = factory.createParser(line)?.readValueAsTree()
            ?: throw JsonParsingException(line, "failed at parsing line as JSON")

    if (!tree.isObject) throw JsonParsingException(line, "expected object at top level")

    // Lines with objects that have init or done fields are for debugging; they should be discarded.
    if ((tree.get("init") != null) || (tree.get("done") != null)) {
        return null
    }

    val number = tree.get("envelope")?.get("from")?.get("number")?.asText()
            ?: throw JsonParsingException(line, "failed to find envelope.from.number")
    val phoneNumber = PhoneNumber(number)

    val body = tree.get("data")?.get("body")?.asText()?.trim()
            ?: throw JsonParsingException(line, "failed to find data.body")

    // We expect the body to just be the user's email address.
    if (!emailValidator.isValid(body)) {
        throw JsonParsingException(line, "message body is not a valid email address")
    }

    return RegistrationMessage(phoneNumber, body)
}
