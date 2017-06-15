package keymonitor.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


val DEFAULT_DB_FILE = "database.sqlite"
var DB_FILE = DEFAULT_DB_FILE

val connection: Connection by lazy {
    DriverManager.getConnection("jdbc:sqlite:${DB_FILE}")
            ?: throw SQLException("failed to connect to database")
}
