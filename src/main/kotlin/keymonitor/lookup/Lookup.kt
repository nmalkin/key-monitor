package keymonitor.lookup

import keymonitor.database.Key
import keymonitor.database.LookupTask

/**
 * Executes the given lookup task, saving the result to the database and returning a Key object
 *
 * At the end, the task will be marked as completed
 */
fun performLookup(task: LookupTask, api: SignalAPI): Key {
    throw NotImplementedError()
}
