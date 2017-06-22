package keymonitor.lookup

import keymonitor.common.CONFIGS
import keymonitor.common.PhoneNumber
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.api.push.TrustStore
import org.whispersystems.signalservice.internal.push.PushServiceSocket
import org.whispersystems.signalservice.internal.push.SignalServiceUrl
import org.whispersystems.signalservice.internal.util.StaticCredentialsProvider
import java.io.InputStream
import java.security.Security

fun lookup(phoneNumber: PhoneNumber) {
    /*
     * The URL of the official Signal server
     */
    val serverURL = "https://textsecure-service.whispersystems.org"
    /*
     * TrustStore is "A class that represents a Java {@link java.security.KeyStore} and
     * its associated password."
     * This file has been imported directly from the Signal Android client.
     */
    val trustStore = object : TrustStore {
        override fun getKeyStoreInputStream(): InputStream {
            return javaClass.getResourceAsStream("whisper.store")
        }

        override fun getKeyStorePassword() = "whisper"
    }
    /*
     * Signal uses the BKS keystore type (http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore)
     * which is provided by BouncyCastle.
     * To make sure it's available, we need to explicitly provide it.
     * This technique is borrowed from AsamK's signal-cli.
     */
    Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 1)

    val signalServiceUrls = arrayOf(SignalServiceUrl(serverURL, trustStore))

    /*
     * By convention or requirement, the username is the phone number.
     */
    val user = CONFIGS.SIGNAL_PHONE_NUMBER
    val password = "<PASSWORD>"

    /*
     * 52 random bytes.  A 32 byte AES key and a 20 byte Hmac256 key, concatenated.
     */
    val signalingKey = "<KEY>"
    val credentialsProvider = StaticCredentialsProvider(user, password,
            signalingKey)

    /*
     * The user agent can be anything; it just has to be set to something.
     */
    val userAgent = "Key Monitor 0.0.0"

    val socket = PushServiceSocket(signalServiceUrls, credentialsProvider,
            userAgent)

    /* The number to look up.
     * SignalServiceAddress takes a "e164 representation of a phone number"
     */
    val destination = SignalServiceAddress(phoneNumber.toString())

    /*
    * Lookup requires a device ID.
    * Not quite sure what that is.
    * Real implementation (SignalServiceMessageSender:::getEncryptedMessages) first uses
    * the DEFAULT_DEVICE_ID but then looks up others in the store.
    */
    val deviceId = SignalServiceAddress.DEFAULT_DEVICE_ID

    val preKeys = socket.getPreKeys(destination, deviceId)

    preKeys.forEach({
        val identityKey = it.identityKey
        val fingerprint = identityKey.fingerprint

        println("The fingerprint is $fingerprint")
    })
}

fun main(args: Array<String>) {
    val PHONE_NUMBER = PhoneNumber("+15105550123")
    lookup(PHONE_NUMBER)
}
