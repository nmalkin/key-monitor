package keymonitor.lookup

import keymonitor.common.CONFIGS
import keymonitor.database.*
import java.time.Instant

/**
 * Executes the given lookup task, saving the result to the database and returning a Key object
 *
 * At the end, the task will be marked as completed
 */
fun performLookup(task: LookupTask, api: SignalAPI): Key {
    // Figure out the number we need to look up
    val user = getUser(task.userID)
            ?: throw DataStateError("invalid user ${task.userID} in task $task")
    val number = user.phoneNumber

    // Perform the lookup
    val rawKeys = api.lookup(number)

    // Note that we record the lookup time *after* the lookup returns
    val lookupTime = Instant.now()

    // Save the new key value to the database
    val key = saveKey(task,
            lookupTime = lookupTime,
            lookupIP = CONFIGS.LOOKUP_IP_ADDRESS,
            lookupPhone = api.user,
            value = toKeyList(rawKeys))

    // Now that we've been able to save the value successfully, the lookup task is truly complete
    task.status = LookupTaskStatus.COMPLETED
    task.save()

    return key
}
