package keymonitor.common

import com.beust.jcommander.ParameterException
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull


class PhoneNumberTest : Spek({
    val badNumber = "666"
    val goodNumber = "+18885550123"

    describe("a phone number") {
        on("creation") {
            it("throws an exception if initialized with invalid number") {
                assertFailsWith<IllegalArgumentException> {
                    PhoneNumber(badNumber)
                }
            }

            it("doesn't do that if given a good number") {
                val number = PhoneNumber(goodNumber)
                assertNotNull(number)
            }
        }
    }

    describe("the JCommander validator") {
        it("throws a ParameterException for bad numbers") {
            assertFailsWith<ParameterException> {
                PhoneNumberValidator().validate("dummy", badNumber)
            }
        }
    }
})
