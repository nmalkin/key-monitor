package keymonitor.common

import keymonitor.database.Database
import keymonitor.database.setup
import java.io.File


/**
 * Set up a new database for testing
 *
 * @return the file storing the database
 */
fun useTestingDatabase(): File {
    val tempFile = File.createTempFile("testing-database", ".sqlite")
    tempFile.deleteOnExit()
    Database.file = tempFile.absolutePath
    setup()
    return tempFile
}

/**
 * Clean up a database after testing
 *
 * @param databaseFile the file where the database is stored
 */
fun closeTestingDatabase(databaseFile: File?) {
    Database.closeConnection()
    databaseFile?.delete()
}