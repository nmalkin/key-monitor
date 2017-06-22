package keymonitor.common

import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

private val client = OkHttpClient()

/**
 * Send an email with the provided parameters
 *
 * Uses the Mailgun API to do so
 *
 * @throws IOException if the API call is unsuccessful, including if authorization fails
 */
fun sendMessage(email: String, subject: String, body: String) {
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
        if (response.code() == 401) throw IOException("invalid credentials")
        else if (!response.isSuccessful) throw IOException("Unexpected code " + response)
    }
}
