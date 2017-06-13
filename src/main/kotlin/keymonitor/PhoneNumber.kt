package keymonitor

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.IStringConverter
import com.beust.jcommander.ParameterException
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter

fun validPhoneNumber(number: String): Boolean {
    return PhoneNumberFormatter.isValidNumber(number)
}

open class PhoneNumber(val number: String) {
    init {
        if (!validPhoneNumber(number)) {
            throw IllegalArgumentException("invalid phone number: $number")
        }
    }

    override fun toString(): String {
        return number
    }
}

// for JCommander
class PhoneNumberValidator : IParameterValidator {
    override fun validate(name: String, value: String) {
        if (!validPhoneNumber(value)) throw ParameterException("invalid phone number provided")
    }
}

class PhoneNumberBuilder : IStringConverter<PhoneNumber> {
    override fun convert(value: String): PhoneNumber {
        return PhoneNumber(value)
    }
}