package keymonitor.unsubscribe

import org.jetbrains.ktor.application.Application
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.jetty.Jetty
import org.jetbrains.ktor.response.respondText
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.routing

/** The unsubscribe web service */
fun Application.main() {
    routing {
        get("/") {
            val token = call.parameters["t"]
            if (token == null) {
                call.respondText("missing token")
            } else {
                try {
                    val unsubscribe = processUnsubscribe(token)
                    when (unsubscribe) {
                        UnsubscribeResult.FAIL -> {
                            call.response.status(HttpStatusCode.BadRequest)
                            call.respondText("We're sorry, the link you clicked on is valid.")
                        }
                        UnsubscribeResult.SUCCESS -> {
                            call.response.status(HttpStatusCode.OK)
                            call.respondText("Thanks, we've processed your unsubscribe request.")
                        }
                    }
                } catch (e: Exception) {
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.respondText("We're sorry, something went wrong.")
                }
            }
        }
    }
}

/**
 * Launch the unsubscribe web process in a Jetty container
 *
 * @param port the port to listen on
 */
fun launch(port: Int) {
    embeddedServer(Jetty, port, module = Application::main)
            .start(wait = true)
}

fun main(args: Array<String>) {
    launch(8080)
}