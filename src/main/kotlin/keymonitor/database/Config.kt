package keymonitor.database

import keymonitor.common.CONFIGS.DEFAULT_DB_FILE
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/** Singleton providing database configuration and access */
object Database {
    /**
     * The path to the file backing our SQLite database
     *
     * You can change it if you don't want to use the default one;
     * if you do, any existing connection will be closed.
     */
    var file = DEFAULT_DB_FILE
        set(value) {
            field = value

            // Invalidate currently cached connection
            closeConnection()
        }

    /** Backing property for the database connection */
    private var _connection: Connection? = null

    /**
     * A Connection to the database
     *
     * If one doesn't exist at access-time, it will be created.
     * Otherwise, an existing one will be returned.
     */
    val connection: Connection
        get() {
            if (_connection == null) {
                _connection = DriverManager.getConnection("jdbc:sqlite:$file")
                        ?: throw SQLException("failed to connect to database")
            }

            return _connection ?: throw AssertionError("Set to null by another thread")
        }

    /**
     * Close the connection and remove references to the underlying object
     */
    fun closeConnection() {
        _connection?.close()
        _connection = null
    }
}

/** Thrown when the data we're operating on appears to be in a bad state */
class DataStateError(message: String) : RuntimeException(message)