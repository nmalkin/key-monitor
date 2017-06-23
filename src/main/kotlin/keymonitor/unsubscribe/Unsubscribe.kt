package keymonitor.unsubscribe

import keymonitor.database.*


enum class UnsubscribeResult { SUCCESS, FAIL }

/**
 * Processes an unsubscribe request for the given token
 *
 * To unsubscribe, the email identified by the token is marked as EmailStatus.UNSUBSCRIBED,
 * and the associated user is marked as UserStatus.DEACTIVATED.
 *
 * @return FAIL if no email could be found with this token
 */
fun processUnsubscribe(unsubscribeToken: String): UnsubscribeResult {
    val email = getEmail(unsubscribeToken) ?: return UnsubscribeResult.FAIL

    email.status = EmailStatus.UNSUBSCRIBED
    email.save()

    val user = getUser(email.userID) ?: return UnsubscribeResult.FAIL
    user.status = UserStatus.DEACTIVATED
    user.save()

    return UnsubscribeResult.SUCCESS
}