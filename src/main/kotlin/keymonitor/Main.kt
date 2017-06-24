package keymonitor

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import keymonitor.common.CONFIGS
import keymonitor.database.Database

@Parameters()
class MainCommand {
    @Parameter(names = arrayOf("-h", "--help"), help = true, description = "Display this help text")
    var help: Boolean = false
}

@Parameters(commandDescription = "Look up keys for all pending tasks")
class LookupCommand

@Parameters(commandDescription = "Generate lookup tasks")
class ScheduleCommand

@Parameters(commandDescription = "Check for messages and register any new users")
class SignupCommand

@Parameters(commandDescription = "Set up the database for Key Monitor")
class SetupDatabaseCommand

@Parameters(commandDescription = "Run the unsubscribe web service")
class UnsubscribeServiceCommand

fun main(args: Array<String>) {
    val mainCommand = MainCommand()
    val commands = JCommander.newBuilder()
            ?.addObject(mainCommand)
            ?.addCommand("lookup", LookupCommand())
            ?.addCommand("schedule", ScheduleCommand())
            ?.addCommand("setup-database", SetupDatabaseCommand())
            ?.addCommand("signup", SignupCommand())
            ?.addCommand("unsubscribe", UnsubscribeServiceCommand())
            ?.build()
            ?: throw RuntimeException("failed to initialize arg parser")
    commands.parse(*args)

    if (mainCommand.help) {
        commands.usage()
        return
    }

    when (commands.parsedCommand) {
        "lookup" -> keymonitor.lookup.run()
        "schedule" -> keymonitor.schedule.run()
        "setup-database" -> keymonitor.database.setup(verbose = true)
        "signup" -> keymonitor.signup.run()
        "unsubscribe" -> keymonitor.unsubscribe.launch(CONFIGS.UNSUBSCRIBE_PORT.toInt())
        else -> commands.usage()
    }

    // By this point, we're done with any commands and ready to exit,
    // so close any lingering database connections
    Database.closeConnection()
}
