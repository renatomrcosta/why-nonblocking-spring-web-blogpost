package de.renatomrcosta.whynonblockingspringweb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest(
    @LocalServerPort private val port: Int
) : DescribeSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {
        val webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()

        describe("Greeting API Integration Tests") {
            describe("GET /greeting") {
                it("should return all greetings") {
                    webTestClient.get()
                        .uri("/greeting")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk
                        .expectBodyList<Greeting>()
                        .hasSize(3)
                }
            }

            describe("GET /greeting/{id}") {
                it("should return a greeting when it exists") {
                    webTestClient.get()
                        .uri("/greeting/1")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk
                }

                it("should return 404 when greeting does not exist") {
                    webTestClient.get()
                        .uri("/greeting/999")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isNotFound
                }
            }
        }
    }

    // We're not using a TestConfiguration to avoid bean definition override exceptions
    // Instead, we're testing the endpoints directly without mocking the HttpClient
}
