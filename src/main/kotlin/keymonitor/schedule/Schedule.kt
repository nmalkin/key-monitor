package keymonitor.schedule

import keymonitor.common.CONFIGS
import keymonitor.database.LookupTask
import keymonitor.database.User
import keymonitor.database.createTask
import java.time.Duration
import java.time.Instant
import java.util.*

/** How often, on average, to look up keys (in minutes) */
val LOOKUP_FREQUENCY = CONFIGS.LOOKUP_FREQUENCY.toInt()
val LOOKUP_INTERVAL = Duration.ofMinutes(LOOKUP_FREQUENCY.toLong())!!

private val random = Random()

/**
 * Schedules a task for given user during the defined interval
 *
 * The task will be scheduled for a random time between the given start time
 * and LOOKUP_FREQUENCY later.
 *
 * A task expires LOOKUP_FREQUENCY minutes after it is scheduled.
 *
 * @param user the user whose key to look up
 * @param startTime the earliest the lookup should run
 *
 * @return the new task after saving it to the database
 */
fun scheduleTaskForUser(user: User, startTime: Instant): LookupTask {
    // Choose an offset in the interval [1, LOOKUP_FREQUENCY - 1]
    val offset = random.nextInt(LOOKUP_FREQUENCY - 1) + 1

    val lookupTime = startTime.plus(Duration.ofMinutes(offset.toLong()))
    val expires = lookupTime.plus(LOOKUP_INTERVAL)

    val newTask = createTask(user, lookupTime, expires)
    return newTask
}
