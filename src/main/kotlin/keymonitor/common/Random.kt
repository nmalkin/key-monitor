package keymonitor.common

import java.security.SecureRandom

private val generator = SecureRandom()

/** Encodes given bytes as a hex string. */
internal fun encodeBytesAsHex(bytes: ByteArray): String {
    return bytes.map { String.format("%02X", it) }
            .joinToString(separator = "")
}

/** Returns a hex-encoded string with the given number of random bytes. */
fun getRandomHex(numBytes: Int = 16): String {
    val bytes = ByteArray(numBytes)
    generator.nextBytes(bytes)

    return encodeBytesAsHex(bytes)
}