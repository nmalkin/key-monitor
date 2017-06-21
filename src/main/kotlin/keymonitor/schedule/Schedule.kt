package keymonitor.schedule

import keymonitor.common.CONFIGS
import keymonitor.database.LookupTask
import keymonitor.database.User
import java.time.Duration
import java.time.Instant

/** How often, on average, to look up keys (in minutes) */
var LOOKUP_FREQUENCY = Duration.ofMinutes(CONFIGS.LOOKUP_FREQUENCY.toLong())

/**
 * Schedules a task for given user during the defined interval
 *
 * The task will be scheduled for a random time between the given start time
 * and LOOKUP_FREQUENCY later.
 *
 * @param user the user whose key to look up
 * @param startTime the earliest the lookup should run
 *
 * @return the new task after saving it to the database
 */
fun scheduleTaskForUser(user: User, startTime: Instant): LookupTask {
    throw NotImplementedError()
}
