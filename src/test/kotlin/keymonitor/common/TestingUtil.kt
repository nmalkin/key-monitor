package keymonitor.common

import keymonitor.database.Database
import keymonitor.database.setup
import java.io.File
import java.sql.ResultSet


/**
 * Set up a new database for testing
 */
fun useNewTestingDatabase() {
    val tempFile = File.createTempFile("testing-database", ".sqlite")
    tempFile.deleteOnExit()
    Database.file = tempFile.absolutePath
    setup()
}

/**
 * Clean up database after testing
 */
fun closeTestingDatabase() {
    Database.closeConnection()
}

/** Shorthand for executing a query with the current database connection */
fun query(sql: String): ResultSet {
    return Database.connection.createStatement().executeQuery(sql)
}