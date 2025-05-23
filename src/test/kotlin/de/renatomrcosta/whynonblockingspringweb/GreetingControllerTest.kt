package de.renatomrcosta.whynonblockingspringweb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [GreetingController::class])
@Import(TestConfig::class)
class GreetingControllerTest(
    private val mockMvc: MockMvc,
    private val greetingService: GreetingService
) : DescribeSpec({

    // SpringExtension is added in the class's extensions() method

    describe("GreetingController") {
        describe("GET /greeting") {
            it("should return all greetings") {
                // Setup
                val greetings = listOf(
                    Greeting(id = 1, text = "Hello there!"),
                    Greeting(id = 2, text = "Howdy Partner!"),
                    Greeting(id = 3, text = "Well, that's a fine how do you do!")
                )
                coEvery { greetingService.getAll() } returns greetings

                // When/Then
                mockMvc.perform(
                    get("/greeting")
                        .accept(MediaType.APPLICATION_JSON)
                )
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].text").value("Hello there!"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].text").value("Howdy Partner!"))
                    .andExpect(jsonPath("$[2].id").value(3))
                    .andExpect(jsonPath("$[2].text").value("Well, that's a fine how do you do!"))
            }
        }

        describe("GET /greeting/{id}") {
            it("should return a greeting when it exists") {
                // Setup
                val greeting = Greeting(id = 1, text = "Hello there!")
                coEvery { greetingService.getById(1) } returns greeting

                // When/Then
                mockMvc.perform(
                    get("/greeting/1")
                        .accept(MediaType.APPLICATION_JSON)
                )
                    .andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.text").value("Hello there!"))
            }

            it("should return 404 when greeting does not exist") {
                // Setup
                coEvery { greetingService.getById(999) } returns null

                // When/Then
                mockMvc.perform(
                    get("/greeting/999")
                        .accept(MediaType.APPLICATION_JSON)
                )
                    .andExpect(status().isNotFound)
            }
        }
    }
}) {
    override fun extensions() = listOf(SpringExtension)
}
