package com.ultish.jikangaaruserver.dataFetchers

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import com.querydsl.core.BooleanBuilder
import com.ultish.jikangaaruserver.entities.EUser
import com.ultish.jikangaaruserver.repositories.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest(
   classes = [DgsAutoConfiguration::class,
      UserDataFetcher::class]
)
class UserDataFetcherTest {

   @Autowired
   lateinit var dgsQueryExecutor: DgsQueryExecutor

   @MockBean
   lateinit var repository: UserRepository

   @BeforeEach
   fun before() {
      Mockito.`when`(
         repository.findAll(ArgumentMatchers.any(BooleanBuilder::class.java))
      ).thenAnswer {
         listOf(
            EUser(
               id = "1-2-3-4", username = "jxhui", password = "password", trackedDayIds = mutableListOf()
            )
         )
      }
   }

   @Test
   fun users() {
      val userNames: List<String> = dgsQueryExecutor
         .executeAndExtractJsonPath(
            """
         {
            users {
               id
               username
            }
         }
         """.trimIndent(), "data.users[*].username"
         )

      assertThat(userNames.contains("jxhui"))
   }

   @Test
   fun emptyArgs() {
      val userNames: List<String> = dgsQueryExecutor
         .executeAndExtractJsonPath(
            """
         {
            users(username: "") {
               id
               username
            }
         }
         """.trimIndent(), "data.users[*].username"
         )
      assertThat(userNames.isEmpty())
   }

   @Test
   fun exceptional() {
      Mockito.`when`(
         repository.findAll(
            ArgumentMatchers.any(
               BooleanBuilder::class.java
            )
         )
      ).thenThrow(RuntimeException("how exceptional"))

      val result = dgsQueryExecutor.execute(
         """
            {
               users {
                  id
               }
            }
         """.trimIndent()
      )

      assertThat(result.errors).isNotEmpty
      assertThat(result.errors[0].message).isEqualTo(
         "java.lang" +
            ".RuntimeException: how exceptional"
      )
   }
}