package de.renatomrcosta.whynonblockingspringweb

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WhyNonBlockingSpringWebApplicationTests : DescribeSpec({
    describe("Spring Application Context") {
        it("should load successfully") {
            // This test will pass if the Spring context loads successfully
            true shouldBe true
        }
    }
})
