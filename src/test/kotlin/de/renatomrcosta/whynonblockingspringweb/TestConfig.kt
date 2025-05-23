package de.renatomrcosta.whynonblockingspringweb

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfig {
    
    @Bean
    fun greetingService(): GreetingService = mockk(relaxed = true)
}