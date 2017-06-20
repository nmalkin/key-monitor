package keymonitor.unsubscribe

import org.jetbrains.ktor.jetty.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.host.*
import org.jetbrains.ktor.response.*

fun main(args: Array<String>) {
    embeddedServer(Jetty, 8080) {
        routing {
            get("/") {
                val token = call.parameters["t"]
                if (token == null) {
                    call.respondText("missing token")
                } else {
                    call.respondText("unsubscribe token: $token!")
                }
            }
        }
    }.start(wait = true)
}