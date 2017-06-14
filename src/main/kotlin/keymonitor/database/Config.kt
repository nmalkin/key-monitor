package keymonitor.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException


val DB_FILE = "database.sqlite"

val connection: Connection by lazy {
    DriverManager.getConnection("jdbc:sqlite:${DB_FILE}")
            ?: throw SQLException("failed to connect to database")
}
