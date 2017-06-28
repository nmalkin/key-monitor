package keymonitor.unsubscribe

import keymonitor.common.logger
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
    logger.info("processing unsubscribe request with token $unsubscribeToken")

    val email = getEmail(unsubscribeToken)
    if (email == null) {
        logger.info("invalid unsubscribe token")
        return UnsubscribeResult.FAIL
    }

    email.status = EmailStatus.UNSUBSCRIBED
    email.save()

    val user = getUser(email.userID)
            ?: throw DataStateError("email ${email.id} not associated with valid user")
    user.status = UserStatus.DEACTIVATED
    user.save()

    logger.info("successfully processed unsubscribe")
    return UnsubscribeResult.SUCCESS
}