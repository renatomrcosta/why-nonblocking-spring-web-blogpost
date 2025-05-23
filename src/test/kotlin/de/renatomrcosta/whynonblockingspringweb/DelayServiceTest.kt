package de.renatomrcosta.whynonblockingspringweb

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.test.runBlockingTest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture

@OptIn(ExperimentalCoroutinesApi::class)
class DelayServiceTest : DescribeSpec({
    describe("DelayService") {
        // Mock the Java HTTP client
        val javaClient = mockk<HttpClient>()
        // Create the service under test with the mock
        val delayService = DelayService(javaClient)

        describe("delay") {
            it("should make an HTTP request to the delay endpoint") {
                runBlockingTest {
                    // Setup
                    val requestSlot = slot<HttpRequest>()
                    val responseBody = "Response from delay endpoint"
                    val mockResponse = mockk<HttpResponse<String>> {
                        every { statusCode() } returns 200
                        every { body() } returns responseBody
                    }

                    // Mock the async HTTP call
                    every { 
                        javaClient.sendAsync(capture(requestSlot), any<HttpResponse.BodyHandler<String>>()) 
                    } returns CompletableDeferred(mockResponse).asCompletableFuture()

                    // When
                    delayService.delay()

                    // Then
                    verify { javaClient.sendAsync(any(), any<HttpResponse.BodyHandler<String>>()) }

                    // Verify the request was built correctly
                    val capturedRequest = requestSlot.captured
                    capturedRequest.method() shouldBe "GET"
                    capturedRequest.uri() shouldBe URI.create("https://httpbin.org/delay/1")
                }
            }

            it("should handle HTTP errors gracefully") {
                runBlockingTest {
                    // Setup
                    val mockResponse = mockk<HttpResponse<String>> {
                        every { statusCode() } returns 500
                        every { body() } returns "Error"
                    }

                    // Mock the async HTTP call to return an error
                    every { 
                        javaClient.sendAsync(any(), any<HttpResponse.BodyHandler<String>>()) 
                    } returns CompletableDeferred(mockResponse).asCompletableFuture()

                    // When/Then - The test should not throw an exception
                    delayService.delay() // This should complete without throwing

                    // Verify the HTTP call was made
                    verify { javaClient.sendAsync(any(), any<HttpResponse.BodyHandler<String>>()) }
                }
            }
        }
    }
})
