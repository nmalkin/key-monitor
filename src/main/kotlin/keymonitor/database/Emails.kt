package keymonitor.database

val CREATE_EMAIL_TABLE =
        """CREATE TABLE IF NOT EXISTS emails (
           id INTEGER PRIMARY KEY,
           user INTEGER NOT NULL,
           email VARCHAR(128) NOT NULL,
           verification_status VARCHAR(16) NOT NULL,
           unsubscribe_token VARCHAR(256) NOT NULL,

           FOREIGN KEY(user) REFERENCES users(id)
        )
    """
