package keymonitor.common

import kotlin.reflect.KProperty

object CONFIGS {
    /** The URL of the unsubscribe server */
    var UNSUBSCRIBE_SERVER: String by Conf("http://example.com/")
    /** The email address from which the app's communication is coming */
    var EMAIL_FROM: String by Conf("Key Monitor <keymonitor@example.com")
    /** The email domain, as used by the Mailgun API */
    var EMAIL_DOMAIN: String by Conf()
    /** The base URL of the Mailgun API */
    var MAILGUN_URL: String by Conf("https://api.mailgun.net/")
    /** Mailgun API key */
    var MAILGUN_API_KEY: String by Conf()
}

/** A [delegate class](https://kotlinlang.org/docs/reference/delegated-properties.html)
 * that lazy-loads config values from the environment.
 *
 * Usage:
 *
 *      object MY_CONFIGS {
 *          var SETTING: String by Conf("default value")
 *      }
 *
 * Now, when you access MY_CONFIGS.SETTING, the class will return the value of the environment
 * variable SETTING. If that doesn't exist, it will return the default value.
 * If *that* value wasn't provided, an exception is thrown, but only on access time.
 *
 * Fun fact: if this class is named Config, it doesn't seem to work: Kotlin claims an "unresolved reference."
 *
 * @constructor takes a default value to return if the property isn't available in the environment
 * @throws RuntimeException at access-time if no value exists for property
 */
class Conf(val default: String? = null) {
    private var value: String? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        if (value == null) {
            // This property hasn't been set yet.
            // Try to load it from the environment, or use the default.
            value = System.getenv(property.name) ?: default
        }

        return value
                ?: throw RuntimeException("missing config: ${property.name}")
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) {
        value = newValue
    }
}
