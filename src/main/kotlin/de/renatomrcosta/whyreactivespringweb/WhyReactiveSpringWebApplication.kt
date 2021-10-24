package de.renatomrcosta.whyreactivespringweb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootApplication
class WhyReactiveSpringWebApplication

fun main(args: Array<String>) {
    runApplication<WhyReactiveSpringWebApplication>(*args)
}

@RestController
@RequestMapping("/greeting")
class GreetingController(
    private val greetingService: GreetingService,
) {

    @GetMapping
    fun listAll(): List<Greeting> = greetingService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Int): ResponseEntity<Greeting> =
        greetingService.getById(id = id)?.let { greeting ->
            ResponseEntity.ok(greeting)
        } ?: ResponseEntity.notFound().build()
}

@Service
class GreetingService(
    private val delayService: DelayService,
) {
    private val hardCodedGreetings = listOf(
        Greeting(id = 1, text = "Hello there!"),
        Greeting(id = 2, text = "Howdy Partner!"),
        Greeting(id = 3, text = "Well, that's a fine how do you do!"),
    )

    fun getAll(): List<Greeting> {
        trace("Starting work to get all greetings")

        delayService.delay()

        return hardCodedGreetings.also {
            trace("Got all greetings!")
        }
    }

    fun getById(id: Int): Greeting? {
        trace("Starting work to get a specific greeting")

        delayService.delay()

        return hardCodedGreetings.firstOrNull { it.id == id }.also {
            trace("Got specific greeting")
        }
    }
}

data class Greeting(val id: Int, val text: String)

private fun trace(msg: Any) {
    println("[${Thread.currentThread().name}] $msg")
}

@Service
class DelayService(
    private val javaClient: java.net.http.HttpClient,
) {
    fun delay() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$ENDPOINT_URL/$DELAY_SECONDS"))
            .GET()
            .build()

        val future = javaClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())

        future.thenAccept {
            trace("Waiting finished with status ${it.statusCode()}")
        }
    }

    companion object {
        private const val ENDPOINT_URL = "https://httpbin.org/delay"
        private const val DELAY_SECONDS = 1
    }
}

@Configuration
class HttpClientConfiguration {
    @Bean
    fun javaClient(): java.net.http.HttpClient = java.net.http.HttpClient.newHttpClient()
}
