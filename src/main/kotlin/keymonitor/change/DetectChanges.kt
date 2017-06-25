package keymonitor.change

import keymonitor.database.Key
import keymonitor.database.KeyStatus
import keymonitor.database.getLastCheckedKey
import keymonitor.database.saveChange


/** Return true if the values have changed from one key to the next */
fun keyChanged(old: Key, new: Key): Boolean {
    return old.value != new.value
}

/**
 * Check if the given key is different from the last time we saw this user's key
 *
 * To do this, query the database for the last known value of the key.
 *
 * If a change has been detected, save a new KeyChange to the database.
 *
 * On completion, mark the given key as having been checked.
 *
 * If there is no previous data for this key (i.e., it's the first time we're seeing the key from this user)
 * consider it as if no change has been detected.
 *
 * @return whether or not a change was detected
 */
fun checkForChanges(key: Key): Boolean {
    val userID = key.userID
    val lastKey = getLastCheckedKey(userID)

    var changed = false

    if (lastKey != null && keyChanged(lastKey, key)) {
        changed = true
        saveChange(userID, lastKey.id, key.id)
    }

    key.status = KeyStatus.CHECKED
    return changed
}