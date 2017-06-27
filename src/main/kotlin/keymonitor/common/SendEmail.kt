package keymonitor.common

import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.logging.Logger

private val logger = Logger.getLogger("email")

private val client = OkHttpClient()

/**
 * Send an email with the provided parameters
 *
 * Uses the Mailgun API to do so
 *
 * @throws IOException if the API call is unsuccessful, including if authorization fails
 */
fun sendMessage(email: String, subject: String, body: String) {
    logger.info("sending email to $email with subject $subject")

    val formBody = FormBody.Builder()
            .add("from", CONFIGS.EMAIL_FROM)
            .add("to", email)
            .add("subject", subject)
            .add("text", body)
            .build()

    val credential = Credentials.basic("api", CONFIGS.MAILGUN_API_KEY)
    val request = Request.Builder()
            .url("${CONFIGS.MAILGUN_URL}v3/${CONFIGS.EMAIL_DOMAIN}/messages")
            .header("Authorization", credential)
            .post(formBody)
            .build()

    client.newCall(request).execute().use { response ->
        val responseCode = response.code()
        val responseMessage = response.body()?.string()
        logger.info("received response (status code $responseCode): $responseMessage")

        if (responseCode == 400) throw IOException("sending email failed: $responseMessage")
        else if (responseCode == 401) throw IOException("invalid credentials")
        else if (!response.isSuccessful) throw IOException("unexpected response: $responseMessage")
    }
}
