package keymonitor.database


val TABLES = listOf(
        CREATE_USER_TABLE,
        CREATE_EMAIL_TABLE
)

fun setup() {
    val statement = connection.createStatement()

    for (table in TABLES) {
        println(table)
        statement.executeUpdate(table)
    }

    println("Done!")
}
