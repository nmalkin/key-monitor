package keymonitor.schedule

import keymonitor.common.CONFIGS
import keymonitor.database.LookupTask
import keymonitor.database.User
import keymonitor.database.createTask
import keymonitor.database.getActiveUsers
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.logging.Logger

private val logger = Logger.getLogger("schedule")

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

    logger.info("scheduling lookup for $user at $lookupTime")

    val newTask = createTask(user.id, lookupTime)
    return newTask
}

/**
 * Schedule all active users at the given start time
 */
fun scheduleActiveUsers(startTime: Instant) {
    logger.info("scheduling all active users for period starting at $startTime")
    getActiveUsers().map { user ->
        scheduleTaskForUser(user, startTime)
    }
}

/**
 * Schedule all active users, using the current instant as the start time
 */
fun run() {
    scheduleActiveUsers(Instant.now())
}
