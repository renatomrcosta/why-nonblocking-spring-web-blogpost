package de.renatomrcosta.whynonblockingspringweb

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingServiceTest : DescribeSpec({
    describe("GreetingService") {
        // Mock the DelayService to avoid actual HTTP calls during tests
        val delayService = mockk<DelayService>()
        // Create the service under test with the mock
        val greetingService = GreetingService(delayService)

        // Expected data
        val expectedGreetings = listOf(
            Greeting(id = 1, text = "Hello there!"),
            Greeting(id = 2, text = "Howdy Partner!"),
            Greeting(id = 3, text = "Well, that's a fine how do you do!")
        )

        beforeTest {
            // Setup the mock to do nothing when delay is called
            coEvery { delayService.delay() } returns Unit
        }

        describe("getAll") {
            it("should return all greetings") {
                runBlockingTest {
                    // When
                    val result = greetingService.getAll()

                    // Then
                    result shouldContainExactly expectedGreetings
                    coVerify(exactly = 1) { delayService.delay() }
                }
            }
        }

        describe("getById") {
            it("should return the correct greeting when ID exists") {
                runBlockingTest {
                    // When
                    val result = greetingService.getById(1)

                    // Then
                    result shouldBe expectedGreetings[0]
                    coVerify(exactly = 1) { delayService.delay() }
                }
            }

            it("should return null when ID does not exist") {
                runBlockingTest {
                    // When
                    val result = greetingService.getById(999)

                    // Then
                    result shouldBe null
                    coVerify(exactly = 1) { delayService.delay() }
                }
            }
        }
    }
})
