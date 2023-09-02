package com.ultish.jikangaaruserver.chargeCodes

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import com.ultish.jikangaaruserver.JikanGaAruServerApplication
import com.ultish.jikangaaruserver.entities.EChargeCode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.jpa.domain.Specification

// Create a custom ArgumentMatcher to match Specification instances based on their generic type
class SpecificationMatcher<T>(private val expectedType: Class<T>) : ArgumentMatcher<Specification<T>> {
   override fun matches(argument: Specification<T>?): Boolean {
      // Check if the argument is non-null and its type matches the expectedType
      return argument != null && argument.javaClass == expectedType
   }
}

@SpringBootTest(
   classes = [
      DgsAutoConfiguration::class,
      JikanGaAruServerApplication::class,
      ChargeCodeService::class]
)
class ChargeCodeServiceTest {

   @Autowired
   lateinit var dgsQueryExecutor: DgsQueryExecutor

   @MockBean
   lateinit var repository: ChargeCodeRepository

   @BeforeEach
   fun before() {

      Mockito.`when`(
         repository.findAll(Mockito.argThat(SpecificationMatcher(EChargeCode::class.java)))
      ).thenAnswer {
         listOf(
            EChargeCode(
               id = "1-2-3-4",
               name = "ABCD",
               code = "AB-CD",
               expired = false,
               description = null
            )
         )
      }
   }

   @Test
   fun chargecodes() {
      val chargecodes: List<String> = dgsQueryExecutor.executeAndExtractJsonPath(
         """
            {
               chargeCodes {
                  code
               }
            }
         """.trimIndent(), "data.chargeCodes[*].code"
      )

      Assertions.assertThat(chargecodes.contains("AB-CD"))
   }

}