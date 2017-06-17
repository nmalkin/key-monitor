package keymonitor.common

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class ConfigTest : Spek({
    describe("the Conf delegate class") {
        on("the config exists in the environment") {
            it("gets values from the environment") {
                // This test depends on a environment variable that exists.
                // PATH is a good bet for this, but there's a chance it doesn't exist.
                if (System.getenv("PATH") != null) {
                    val sampleConfig = object {
                        var PATH: String by Conf()
                    }

                    assertEquals(System.getenv("PATH"), sampleConfig.PATH)
                }
            }
        }

        on("the config doesn't exist in the environment") {
            it("throws an exception if no default is provided") {
                val sampleConfig = object {
                    var UNLIKELY_ENVIRONMENT_VARIABLE_NAME: String by Conf()
                }

                assertFailsWith<RuntimeException> {
                    sampleConfig.UNLIKELY_ENVIRONMENT_VARIABLE_NAME
                }
            }

            it("uses a default if one is provided") {
                val sampleConfig = object {
                    var UNLIKELY_ENVIRONMENT_VARIABLE_NAME: String by Conf("yay, defaults!")
                }

                assertEquals("yay, defaults!", sampleConfig.UNLIKELY_ENVIRONMENT_VARIABLE_NAME)
            }
        }
    }
})
