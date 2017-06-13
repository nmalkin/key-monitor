package keymonitor.signup

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import keymonitor.PhoneNumber
import org.apache.commons.validator.routines.EmailValidator
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.stream.Collectors

class JsonParsingException(line: String, message: String) : RuntimeException("$message in $line")

fun parseJsonFile(file: File): Collection<RegistrationMessage> {
    // Prepare to parse output
    val jsonParser = ObjectMapper(JsonFactory())
    val emailValidator = EmailValidator.getInstance()
    fun parse(line: String): RegistrationMessage? {
        try {
            return parseLine(jsonParser, emailValidator, line)
        } catch (e: JsonParsingException) {
            // Notify about any errors, but don't halt
            System.err.println(e)
            return null
        }
    }

    // Parse the file, filtering out any lines that don't return anything
    val reader = BufferedReader(FileReader(file))
    val messages: MutableCollection<RegistrationMessage> = reader.lines()
            // Each line is parsed separately, because log file format is one JSON entry per line
            .map(::parse)
            .filter { it != null }
            .collect(Collectors.toSet())

    return messages
}

fun parseLine(mapper: ObjectMapper, emailValidator: EmailValidator, line: String): RegistrationMessage? {
    val tree: JsonNode
    try {
        tree = mapper.readTree(line)
                ?: throw JsonParsingException(line, "failed at parsing line as JSON")
    } catch (e: JsonParseException) {
        // Catch library's parse exception and re-throw as our own
        throw(JsonParsingException(line, e.message!!))
    }

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