package keymonitor.unsubscribe


enum class UnsubscribeResult { SUCCESS, FAIL }

/**
 * Processes an unsubscribe request for the given token
 */
fun processUnsubscribe(unsubscribeToken: String): UnsubscribeResult {
    return UnsubscribeResult.FAIL
}