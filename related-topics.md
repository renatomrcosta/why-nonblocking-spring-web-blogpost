# Related Topics Building on Non-Blocking Spring Web

Based on the existing implementation and theme of non-blocking Spring Web applications using Kotlin coroutines, here are several related topics that could be explored further, along with abstracts for potential talks or blog posts.

## 1. Comparing Reactive Frameworks: Spring WebFlux vs Kotlin Coroutines

**Abstract:** This talk explores the differences, similarities, and interoperability between Spring WebFlux's reactive programming model and Kotlin's coroutines. We'll compare performance characteristics, developer experience, and code complexity across various use cases. By the end, attendees will understand when to choose WebFlux, when to use coroutines, and how to leverage the strengths of both approaches in different scenarios.

## 2. Testing Non-Blocking Spring Applications

**Abstract:** Testing asynchronous and non-blocking code presents unique challenges. This session demonstrates effective testing strategies for non-blocking Spring applications using Kotlin coroutines. We'll cover unit testing suspend functions, integration testing of reactive endpoints, simulating high concurrency in tests, and tools for debugging asynchronous flows. Practical examples will show how to write tests that are both reliable and maintainable.

## 3. Optimizing Database Access in Non-Blocking Applications

**Abstract:** The benefits of non-blocking web layers can be negated by blocking database operations. This talk explores approaches to fully non-blocking database access in Spring applications, including R2DBC, reactive MongoDB, and other asynchronous data access technologies. We'll examine the performance implications, migration strategies from JDBC, and patterns for effectively modeling domain logic in a reactive data environment.

## 4. Error Handling and Resilience in Non-Blocking Spring Applications

**Abstract:** Non-blocking applications require different approaches to error handling and resilience. This session covers advanced error handling techniques in Kotlin coroutines and Spring Web, including structured concurrency, supervisorScope, exception propagation across asynchronous boundaries, and integration with resilience libraries like Resilience4j. We'll demonstrate patterns for graceful degradation and maintaining system stability under failure conditions.

## 5. Scaling Non-Blocking Applications: From Development to Production

**Abstract:** This talk bridges the gap between development and operations for non-blocking Spring applications. We'll explore monitoring, metrics collection, and observability concerns specific to asynchronous applications. Topics include tracing requests across asynchronous boundaries, meaningful metrics for non-blocking systems, right-sizing infrastructure, and identifying performance bottlenecks. Real-world case studies will demonstrate successful scaling strategies.

## 6. Beyond HTTP: Non-Blocking Messaging and Event-Driven Architectures

**Abstract:** Non-blocking principles extend beyond HTTP endpoints. This session explores how to build fully reactive event-driven systems using Spring, Kotlin coroutines, and messaging technologies like Kafka or RabbitMQ. We'll demonstrate patterns for non-blocking message processing, backpressure handling, and maintaining consistency in distributed asynchronous systems.

## 7. Migration Strategies: From Blocking to Non-Blocking Spring Applications

**Abstract:** Transitioning existing applications from traditional blocking models to non-blocking architectures presents significant challenges. This practical talk provides a roadmap for incremental migration, identifying good candidates for initial conversion, maintaining compatibility during transition periods, and avoiding common pitfalls. We'll share lessons learned from real-world migration projects and techniques for measuring the impact of changes.