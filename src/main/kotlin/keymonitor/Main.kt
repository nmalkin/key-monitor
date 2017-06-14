package keymonitor

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters

@Parameters()
class MainCommand {
    @Parameter(names = arrayOf("-h", "--help"), help = true, description = "Displays this help text")
    var help: Boolean = false
}

@Parameters(commandDescription = "Check for messages and register any new users")
class SignupCommand {
    @Parameter(names = arrayOf("--number"), required = true,
            validateWith = arrayOf(PhoneNumberValidator::class),
            converter = PhoneNumberBuilder::class,
            description = "Phone number of the server")
    var serverPhoneNumber: PhoneNumber? = null
}

@Parameters(commandDescription = "Set up the database for Key Monitor")
class SetupDatabaseCommand

fun main(args: Array<String>) {
    val mainCommand = MainCommand()
    val signupCommand = SignupCommand()
    val commands = JCommander.newBuilder()
            ?.addObject(mainCommand)
            ?.addCommand("setup-database", SetupDatabaseCommand())
            ?.addCommand("signup", signupCommand)
            ?.build()
            ?: throw RuntimeException("failed to initialize arg parser")
    commands.parse(*args)

    if (mainCommand.help) {
        commands.usage()
        return
    }

    when (commands.parsedCommand) {
        "setup-database" -> keymonitor.database.setup()
        "signup" -> {
            val serverNumber = signupCommand.serverPhoneNumber ?: throw IllegalArgumentException()
            keymonitor.signup.run(serverNumber)
        }
        else -> commands.usage()
    }

}
