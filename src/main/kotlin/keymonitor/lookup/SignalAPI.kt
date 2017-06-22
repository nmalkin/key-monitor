package keymonitor.lookup

import keymonitor.common.CONFIGS
import keymonitor.common.PhoneNumber
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.signalservice.api.push.SignalServiceAddress
import org.whispersystems.signalservice.api.push.TrustStore
import org.whispersystems.signalservice.api.util.CredentialsProvider
import org.whispersystems.signalservice.internal.push.PushServiceSocket
import org.whispersystems.signalservice.internal.push.SignalServiceUrl
import org.whispersystems.signalservice.internal.util.StaticCredentialsProvider
import java.io.InputStream
import java.security.Security

typealias Key = ByteArray

class SignalAPI(credentials: CredentialsProvider) {
    /** A factory for the Signal API */
    companion object {
        /** Return the default Signal API, based on the credentials preloaded on the system */
        fun loadDefault(): SignalAPI {
            // The username is the phone number.
            val user = CONFIGS.SIGNAL_PHONE_NUMBER
            val password = "<PASSWORD>"
            val signalingKey = "<KEY>"
            val credentialsProvider = StaticCredentialsProvider(user, password, signalingKey)

            return SignalAPI(credentialsProvider)
        }
    }

    init {
        /*
         * Signal uses the BKS keystore type (http://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#KeyStore)
         * which is provided by BouncyCastle.
         * To make sure it's available, we need to explicitly provide it.
         * This technique is borrowed from AsamK's signal-cli.
         */
        if (Security.getProvider("BKS") == null) {
            Security.insertProviderAt(org.bouncycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }

    /**
     * The URL of the official Signal server
     */
    private val serverURL = "https://textsecure-service.whispersystems.org"

    /**
     * TrustStore is "A class that represents a Java {@link java.security.KeyStore} and its associated password."
     * It contains the certificate of the Signal server, which we should trust.
     * It's stored on disk as a file that has been imported directly from the Signal Android client.
     */
    private val trustStore = object : TrustStore {
        override fun getKeyStoreInputStream(): InputStream {
            return javaClass.getResourceAsStream("whisper.store")
        }

        override fun getKeyStorePassword() = "whisper"
    }

    val signalServiceUrls = arrayOf(SignalServiceUrl(serverURL, trustStore))

    /**
     * The user agent can be anything; it just has to be set to something.
     */
    val userAgent = "Key Monitor 0.0.0"

    /** The socket that we will use for connecting to the Signal server */
    val socket = PushServiceSocket(signalServiceUrls, credentials, userAgent)

    /**
     * Lookup requires a device ID.
     * Not quite sure what that is.
     * Real implementation (SignalServiceMessageSender:::getEncryptedMessages) first uses
     * the DEFAULT_DEVICE_ID but then looks up others in the store.
     */
    val deviceId = SignalServiceAddress.DEFAULT_DEVICE_ID

    /**
     * Query the Signal server for the identity keys associated with the given phone number
     *
     * @param phoneNumber The number to look up.
     */
    fun lookup(phoneNumber: PhoneNumber): Collection<Key> {
        // SignalServiceAddress takes a "e164 representation of a phone number."
        // Because it uses libsignal's validator, our PhoneNumber should already be in that formatt.
        val destination = SignalServiceAddress(phoneNumber.toString())

        // Query the server
        val preKeys = socket.getPreKeys(destination, deviceId)

        val keys: Collection<Key> = preKeys.map({ preKeyBundle ->
            val identityKey: IdentityKey = preKeyBundle.identityKey
            val key: Key = identityKey.serialize()

            key
        })

        return keys
    }
}
