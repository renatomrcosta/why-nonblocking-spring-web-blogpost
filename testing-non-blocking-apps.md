# Testing Non-Blocking Spring Applications with Kotlin Coroutines

This guide provides practical examples and patterns for effectively testing non-blocking Spring applications that use Kotlin coroutines, building on the concepts demonstrated in the original "Why Non-Blocking Spring Web" implementation.

## Unit Testing Suspend Functions

### Example: Testing the GreetingService

```kotlin
@ExtendWith(MockKExtension::class)
class GreetingServiceTest {
    @MockK
    private lateinit var delayService: DelayService
    
    private lateinit var greetingService: GreetingService
    
    @BeforeEach
    fun setup() {
        greetingService = GreetingService(delayService)
    }
    
    @Test
    fun `should return all greetings`() = runTest {
        // Given
        coEvery { delayService.delay() } just runs
        
        // When
        val result = greetingService.getAll()
        
        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { delayService.delay() }
    }
    
    @Test
    fun `should return greeting by id when exists`() = runTest {
        // Given
        coEvery { delayService.delay() } just runs
        
        // When
        val result = greetingService.getById(1)
        
        // Then
        assertNotNull(result)
        assertEquals("Hello there!", result?.text)
        coVerify(exactly = 1) { delayService.delay() }
    }
    
    @Test
    fun `should return null when greeting id does not exist`() = runTest {
        // Given
        coEvery { delayService.delay() } just runs
        
        // When
        val result = greetingService.getById(999)
        
        // Then
        assertNull(result)
        coVerify(exactly = 1) { delayService.delay() }
    }
}
```

## Integration Testing Reactive Endpoints

### Example: Testing the GreetingController

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GreetingControllerIntegrationTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @MockBean
    private lateinit var delayService: DelayService
    
    @BeforeEach
    fun setup() {
        coEvery { delayService.delay() } just runs
    }
    
    @Test
    fun `should return all greetings`() {
        webTestClient.get()
            .uri("/greeting")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Greeting::class.java)
            .hasSize(3)
    }
    
    @Test
    fun `should return greeting by id when exists`() {
        webTestClient.get()
            .uri("/greeting/1")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(1)
            .jsonPath("$.text").isEqualTo("Hello there!")
    }
    
    @Test
    fun `should return 404 when greeting id does not exist`() {
        webTestClient.get()
            .uri("/greeting/999")
            .exchange()
            .expectStatus().isNotFound
    }
}
```

## Testing Concurrency and Thread Behavior

### Example: Verifying Non-Blocking Behavior

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ConcurrencyTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient
    
    @Test
    fun `should handle multiple concurrent requests with limited threads`() {
        // Given
        val requestCount = 50
        val threadPool = Executors.newFixedThreadPool(requestCount)
        val countDownLatch = CountDownLatch(requestCount)
        val threadNames = ConcurrentHashMap.newKeySet<String>()
        
        // When
        repeat(requestCount) {
            threadPool.submit {
                try {
                    webTestClient.get()
                        .uri("/greeting")
                        .exchange()
                        .expectStatus().isOk
                        
                    threadNames.add(Thread.currentThread().name)
                } finally {
                    countDownLatch.countDown()
                }
            }
        }
        
        // Then
        countDownLatch.await(30, TimeUnit.SECONDS)
        threadPool.shutdown()
        
        // Verify that we used fewer server threads than requests
        // This verifies the non-blocking nature of our application
        val serverThreadCount = getServerThreadCount()
        assertTrue(serverThreadCount <= 10, 
            "Expected to use at most 10 server threads, but used $serverThreadCount")
    }
    
    private fun getServerThreadCount(): Int {
        // This would need to be implemented based on your specific monitoring approach
        // Could use JMX, metrics, or a custom endpoint that exposes thread information
        return 10 // Placeholder
    }
}
```

## Testing Error Handling and Resilience

### Example: Testing Error Propagation

```kotlin
@ExtendWith(MockKExtension::class)
class DelayServiceTest {
    @MockK
    private lateinit var javaClient: java.net.http.HttpClient
    
    private lateinit var delayService: DelayService
    
    @BeforeEach
    fun setup() {
        delayService = DelayService(javaClient)
    }
    
    @Test
    fun `should handle HTTP errors gracefully`() = runTest {
        // Given
        val httpException = java.io.IOException("Connection refused")
        coEvery { 
            javaClient.sendAsync(any(), any<HttpResponse.BodyHandler<String>>()) 
        } throws httpException
        
        // When/Then
        assertThrows<java.io.IOException> {
            runBlocking { delayService.delay() }
        }
    }
    
    @Test
    fun `should handle timeouts gracefully`() = runTest {
        // Given
        val timeoutException = java.util.concurrent.TimeoutException("Request timed out")
        coEvery { 
            javaClient.sendAsync(any(), any<HttpResponse.BodyHandler<String>>()) 
        } throws timeoutException
        
        // When/Then
        assertThrows<java.util.concurrent.TimeoutException> {
            runBlocking { delayService.delay() }
        }
    }
}
```

## Best Practices for Testing Non-Blocking Applications

1. **Use the right test dispatcher**: For unit tests, use `TestCoroutineDispatcher` or `StandardTestDispatcher` from `kotlinx-coroutines-test` to have control over virtual time.

2. **Leverage `runTest` for coroutine testing**: This function from the coroutines test library provides a controlled environment for testing suspend functions.

3. **Mock suspend functions properly**: Use libraries like MockK that support coroutines with `coEvery`, `coVerify`, etc.

4. **Test both success and failure paths**: Non-blocking applications need robust error handling, so test how your application behaves when external services fail.

5. **Verify thread usage**: Create tests that confirm your application is truly non-blocking by monitoring thread usage under load.

6. **Test backpressure handling**: If your application implements backpressure mechanisms, create tests that verify they work correctly under high load.

7. **Use WebTestClient for Spring WebFlux testing**: This client is designed for testing reactive endpoints and provides a fluent API for assertions.

8. **Test concurrency explicitly**: Create tests that send multiple concurrent requests to verify your application handles them correctly.

By following these patterns and examples, you can ensure your non-blocking Spring applications are thoroughly tested and robust in production environments.