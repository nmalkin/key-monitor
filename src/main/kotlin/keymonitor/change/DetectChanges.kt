package keymonitor.change

import keymonitor.database.*


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
 * @return the KeyChange object if a change was detected, or null if one wasn't
 */
fun checkForChanges(key: Key): KeyChange? {
    val userID = key.userID
    val lastKey = getLastCheckedKey(userID)

    var changed: KeyChange? = null

    if (lastKey != null && keyChanged(lastKey, key)) {
        changed = saveChange(userID, lastKey.id, key.id)
    }

    key.status = KeyStatus.CHECKED
    return changed
}

/** Check all unchecked keys for changes */
fun run() {
    val unchecked = getAll(KeyStatus.UNCHECKED)
    unchecked.forEach { checkForChanges(it) }
}