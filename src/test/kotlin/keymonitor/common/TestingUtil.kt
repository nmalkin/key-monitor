package keymonitor.common

import keymonitor.database.Database
import keymonitor.database.setup
import java.io.File


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