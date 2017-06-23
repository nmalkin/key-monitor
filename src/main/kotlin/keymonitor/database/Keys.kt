package keymonitor.database

import java.time.Instant

/** Represents whether they has been compared to previous versions for changes */
enum class KeyStatus { UNCHECKED, CHECKED }

/** Represents the results of a key lookup */
data class Key(val id: Int,
               val taskId: Int,
               val userId: Int,
               val lookupTime: Instant,
               val lookupPhone: String,
               val lookupIP: String,
               val status: KeyStatus,
               val value: String)

val CREATE_KEY_TABLE =
        """CREATE TABLE IF NOT EXISTS keys (
            id INTEGER PRIMARY KEY,
            task_id INTEGER NOT NULL,
            user_id INTEGER NOT NULL,
            lookup_time VARCHAR(32) NOT NULL,
            lookup_phone VARCHAR(32) NOT NULL,
            lookup_ip VARCHAR(32) NOT NULL,
            status VARCHAR(32) NOT NULL,
            value VARCHAR(256) NOT NULL,
            created_at VARCHAR(32) DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY(task_id) REFERENCES tasks(id)
            FOREIGN KEY(user_id) REFERENCES users(id)
            )
        """