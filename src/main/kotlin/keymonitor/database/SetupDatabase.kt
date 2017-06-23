package keymonitor.database


/**
 * All the tables that make up our project
 *
 * For easier reference, the schemas are stored alongside each associated class.
 */
private val TABLES = listOf(
        CREATE_USER_TABLE,
        CREATE_EMAIL_TABLE,
        CREATE_TASK_TABLE,
        CREATE_KEY_TABLE
)

/**
 * Set up the database by creating any tables that need to exist
 *
 * Note that we use CREATE TABLE IF EXISTS, so any changes will not be applied automatically.
 * i.e., this doesn't do migrations
 */
fun setup(verbose: Boolean = false) {
    val statement = Database.connection.createStatement()

    for (table in TABLES) {
        if(verbose) println(table)
        statement.executeUpdate(table)
    }

    if(verbose) println("Done!")
}
