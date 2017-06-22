package keymonitor.common

import com.beust.jcommander.IParameterValidator
import com.beust.jcommander.IStringConverter
import com.beust.jcommander.ParameterException
import org.whispersystems.signalservice.api.util.PhoneNumberFormatter

/**
 * Validates if the given string is a valid phone number
 *
 * Defers the actual validation logic to Signal's PhoneNumberFormatter.
 *
 * @return true if it's valid
 */
fun validPhoneNumber(number: String): Boolean {
    return PhoneNumberFormatter.isValidNumber(number)
}

/**
 * Represents a phone number in our project
 * @constructor checks if the initial value is a valid phone number
 * @throws IllegalArgumentException if the initial value is not a valid phone number

 */
open class PhoneNumber(val number: String) {
    init {
        if (!validPhoneNumber(number)) {
            throw IllegalArgumentException("invalid phone number: $number")
        }
    }

    override fun equals(other: Any?): Boolean {
        if(other is PhoneNumber) {
            return number == other.number
        } else {
            return false
        }
    }

    override fun toString(): String {
        return number
    }
}
