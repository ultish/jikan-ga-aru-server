package com.ultish.jikangaaruserver.chargeCodes

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.querydsl.core.BooleanBuilder
import com.ultish.jikangaaruserver.JikanGaAruServerApplication
import com.ultish.jikangaaruserver.entities.EChargeCode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(
   classes = [
//      DgsAutoConfiguration::class,
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
         repository.findAll(ArgumentMatchers.any(BooleanBuilder::class.java))
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