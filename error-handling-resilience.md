# Error Handling and Resilience in Non-Blocking Spring Applications

This guide explores advanced error handling techniques for non-blocking Spring applications using Kotlin coroutines, building on the concepts demonstrated in the original "Why Non-Blocking Spring Web" implementation.

## Understanding Error Propagation in Coroutines

In non-blocking applications, error handling becomes more complex because exceptions can cross asynchronous boundaries. Here's how errors propagate in Kotlin coroutines:

```kotlin
// Example of error propagation in coroutines
suspend fun riskyOperation(): String {
    return withContext(Dispatchers.IO) {
        if (Random.nextBoolean()) {
            throw IOException("Network error")
        }
        "Success"
    }
}

// Calling function
suspend fun performOperation() {
    try {
        val result = riskyOperation()
        println("Operation succeeded: $result")
    } catch (e: IOException) {
        println("Operation failed: ${e.message}")
    }
}
```

## Implementing Resilient Controllers

Let's enhance the existing `GreetingController` with better error handling:

```kotlin
@RestController
@RequestMapping("/greeting")
class ResilientGreetingController(
    private val greetingService: GreetingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    suspend fun listAll(): ResponseEntity<Any> = withErrorHandling {
        greetingService.getAll()
    }

    @GetMapping("/{id}")
    suspend fun getById(@PathVariable id: Int): ResponseEntity<Any> = withErrorHandling {
        greetingService.getById(id = id)?.let { greeting ->
            ResponseEntity.ok(greeting)
        } ?: ResponseEntity.notFound().build()
    }

    private suspend fun <T> withErrorHandling(block: suspend () -> T): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(block())
        } catch (e: TimeoutException) {
            logger.warn("Request timed out", e)
            ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ErrorResponse("Request timed out", HttpStatus.GATEWAY_TIMEOUT.value()))
        } catch (e: IOException) {
            logger.error("I/O error occurred", e)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE.value()))
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value()))
        }
    }

    data class ErrorResponse(val message: String, val status: Int)
}
```

## Using Structured Concurrency for Error Handling

Structured concurrency helps manage the lifecycle of coroutines and ensures proper error propagation:

```kotlin
@Service
class ResilientDelayService(
    private val javaClient: java.net.http.HttpClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    suspend fun delay() = supervisorScope {
        val deferredResponse = async {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$ENDPOINT_URL/$DELAY_SECONDS"))
                    .GET()
                    .build()
                javaClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            } catch (e: Exception) {
                logger.warn("Delay service failed", e)
                throw e
            }
        }
        
        try {
            // Add timeout to the operation
            withTimeout(5000) {
                val response = deferredResponse.await()
                trace("Waiting finished with status ${response.statusCode()}")
            }
        } catch (e: TimeoutCancellationException) {
            // Cancel the underlying job
            deferredResponse.cancel()
            logger.warn("Delay operation timed out")
            throw TimeoutException("Operation timed out after 5 seconds")
        }
    }

    companion object {
        private const val ENDPOINT_URL = "https://httpbin.org/delay"
        private const val DELAY_SECONDS = 1
    }
}
```

## Implementing Circuit Breaker Pattern

The circuit breaker pattern prevents cascading failures by failing fast when a service is unavailable:

```kotlin
@Service
class CircuitBreakerDelayService(
    private val javaClient: java.net.http.HttpClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // Simple in-memory circuit breaker state
    private val state = AtomicReference(CircuitState.CLOSED)
    private val failureCount = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)
    
    suspend fun delay() {
        when (state.get()) {
            CircuitState.OPEN -> {
                // Check if we should try to half-open the circuit
                if (System.currentTimeMillis() - lastFailureTime.get() > RESET_TIMEOUT_MS) {
                    state.set(CircuitState.HALF_OPEN)
                    executeWithCircuitLogic()
                } else {
                    throw CircuitBreakerOpenException("Circuit breaker is open")
                }
            }
            CircuitState.HALF_OPEN -> {
                executeWithCircuitLogic()
            }
            CircuitState.CLOSED -> {
                executeWithCircuitLogic()
            }
        }
    }
    
    private suspend fun executeWithCircuitLogic() {
        try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$ENDPOINT_URL/$DELAY_SECONDS"))
                .GET()
                .build()
            val response = javaClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
            
            // Success - reset failure count and close circuit if it was half-open
            failureCount.set(0)
            if (state.get() == CircuitState.HALF_OPEN) {
                state.set(CircuitState.CLOSED)
            }
            
            trace("Waiting finished with status ${response.statusCode()}")
        } catch (e: Exception) {
            // Record failure
            val currentFailures = failureCount.incrementAndGet()
            lastFailureTime.set(System.currentTimeMillis())
            
            if (currentFailures >= FAILURE_THRESHOLD || state.get() == CircuitState.HALF_OPEN) {
                state.set(CircuitState.OPEN)
                logger.warn("Circuit breaker opened after $currentFailures failures")
            }
            
            throw e
        }
    }
    
    enum class CircuitState {
        CLOSED, OPEN, HALF_OPEN
    }
    
    class CircuitBreakerOpenException(message: String) : RuntimeException(message)
    
    companion object {
        private const val ENDPOINT_URL = "https://httpbin.org/delay"
        private const val DELAY_SECONDS = 1
        private const val FAILURE_THRESHOLD = 5
        private const val RESET_TIMEOUT_MS = 30000 // 30 seconds
    }
}
```

## Implementing Retry Logic

For transient failures, retry logic can help improve resilience:

```kotlin
@Service
class RetryingDelayService(
    private val javaClient: java.net.http.HttpClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    suspend fun delay() = retry(
        times = 3,
        initialDelay = 100,
        maxDelay = 1000,
        factor = 2.0,
        retryOn = { it is IOException || it is TimeoutException }
    ) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$ENDPOINT_URL/$DELAY_SECONDS"))
            .GET()
            .build()
        val response = javaClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        trace("Waiting finished with status ${response.statusCode()}")
    }
    
    // Exponential backoff retry function
    private suspend fun <T> retry(
        times: Int,
        initialDelay: Long,
        maxDelay: Long,
        factor: Double,
        retryOn: (Throwable) -> Boolean,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                if (!retryOn(e)) throw e
                
                logger.warn("Attempt ${attempt + 1} failed, retrying in $currentDelay ms", e)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        // Last attempt
        return block()
    }
    
    companion object {
        private const val ENDPOINT_URL = "https://httpbin.org/delay"
        private const val DELAY_SECONDS = 1
    }
}
```

## Handling Backpressure

In reactive systems, backpressure handling is crucial for preventing resource exhaustion:

```kotlin
@Service
class BackpressureAwareGreetingService(
    private val delayService: DelayService,
) {
    private val semaphore = Semaphore(10) // Limit concurrent operations
    
    suspend fun getAll(): List<Greeting> {
        return withBackpressure {
            trace("Starting work to get all greetings")
            delayService.delay()
            hardCodedGreetings.also {
                trace("Got all greetings!")
            }
        }
    }
    
    suspend fun getById(id: Int): Greeting? {
        return withBackpressure {
            trace("Starting work to get a specific greeting")
            delayService.delay()
            hardCodedGreetings.firstOrNull { it.id == id }.also {
                trace("Got specific greeting")
            }
        }
    }
    
    private suspend fun <T> withBackpressure(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            try {
                // Apply backpressure
                if (!semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                    throw BackpressureException("Too many concurrent requests")
                }
                
                block()
            } finally {
                semaphore.release()
            }
        }
    }
    
    class BackpressureException(message: String) : RuntimeException(message)
    
    private val hardCodedGreetings = listOf(
        Greeting(id = 1, text = "Hello there!"),
        Greeting(id = 2, text = "Howdy Partner!"),
        Greeting(id = 3, text = "Well, that's a fine how do you do!"),
    )
}
```

## Global Error Handling with ControllerAdvice

For consistent error handling across all controllers:

```kotlin
@RestControllerAdvice
class GlobalErrorHandler {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @ExceptionHandler(TimeoutException::class)
    fun handleTimeout(ex: TimeoutException): ResponseEntity<ErrorResponse> {
        logger.warn("Request timed out", ex)
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
            .body(ErrorResponse("Request timed out", HttpStatus.GATEWAY_TIMEOUT.value()))
    }
    
    @ExceptionHandler(IOException::class)
    fun handleIO(ex: IOException): ResponseEntity<ErrorResponse> {
        logger.error("I/O error occurred", ex)
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse("Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE.value()))
    }
    
    @ExceptionHandler(CircuitBreakerDelayService.CircuitBreakerOpenException::class)
    fun handleCircuitOpen(ex: CircuitBreakerDelayService.CircuitBreakerOpenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse("Service is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE.value()))
    }
    
    @ExceptionHandler(BackpressureAwareGreetingService.BackpressureException::class)
    fun handleBackpressure(ex: BackpressureAwareGreetingService.BackpressureException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ErrorResponse("Too many requests", HttpStatus.TOO_MANY_REQUESTS.value()))
    }
    
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR.value()))
    }
    
    data class ErrorResponse(val message: String, val status: Int)
}
```

## Best Practices for Error Handling in Non-Blocking Applications

1. **Use structured concurrency**: Leverage `supervisorScope` and `coroutineScope` to manage the lifecycle of coroutines and ensure proper error propagation.

2. **Implement timeouts**: Always add timeouts to external calls to prevent indefinite waiting.

3. **Apply circuit breakers**: Protect your system from cascading failures by implementing circuit breakers for external dependencies.

4. **Implement retry logic**: Use exponential backoff retry for transient failures.

5. **Handle backpressure**: Limit concurrent operations to prevent resource exhaustion.

6. **Provide meaningful error responses**: Return appropriate HTTP status codes and error messages to clients.

7. **Log errors appropriately**: Use different log levels for different types of errors (warn for transient issues, error for serious problems).

8. **Use global error handlers**: Implement a consistent error handling strategy across your application.

9. **Test error scenarios**: Write tests that simulate failures to ensure your error handling works as expected.

10. **Monitor and alert**: Set up monitoring and alerting for error rates to detect issues early.

By implementing these patterns and practices, you can build resilient non-blocking applications that gracefully handle failures and provide a better user experience.