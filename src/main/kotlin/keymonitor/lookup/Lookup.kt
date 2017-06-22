package keymonitor.lookup

import keymonitor.common.PhoneNumber


fun main(args: Array<String>) {
    val PHONE_NUMBER = PhoneNumber("+15105550123")
    val api = SignalAPI.loadDefault()
    val keys = api.lookup(PHONE_NUMBER)
    keys.forEach { println(it) }
}
