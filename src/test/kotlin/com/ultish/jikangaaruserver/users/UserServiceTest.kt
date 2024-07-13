//package com.ultish.jikangaaruserver.users
//
//import com.netflix.graphql.dgs.DgsQueryExecutor
//import com.querydsl.core.BooleanBuilder
//import com.ultish.jikangaaruserver.JikanGaAruServerApplication
//import com.ultish.jikangaaruserver.entities.EUser
//import com.ultish.jikangaaruserver.trackedDays.TrackedDayRepository
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockito.ArgumentMatchers
//import org.mockito.Mockito
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.MockBean
//
//@SpringBootTest(
//   classes = [
//      // TODO Note: this would start up the whole application (can be expensive). Couldn't use DgsAutoConfiguration
//      //  as it wouldn't load my customContext. There should be a way to rig DgsAutoConfiguration with my
//      //  CustomContext to make this faster. https://github.com/Netflix/dgs-framework/blob/65933df1f26711f7f58c0e1bd1d9885aac1d8dbb/graphql-dgs-spring-boot-oss-autoconfigure/src/test/kotlin/com/netflix/graphql/dgs/autoconfig/testcomponents/MyCustomDgsContextBuilder.kt
//      //  may be an example
//      JikanGaAruServerApplication::class,
//      UserService::class]
//)
//class UserServiceTest {
//
//   @Autowired
//   lateinit var dgsQueryExecutor: DgsQueryExecutor
//
//   @MockBean
//   lateinit var repository: UserRepository
//
//   @MockBean
//   lateinit var trackedDayRepository: TrackedDayRepository
//
//   @BeforeEach
//   fun before() {
//      Mockito.`when`(
//         repository.findAll(ArgumentMatchers.any(BooleanBuilder::class.java))
//      ).thenAnswer {
//         listOf(
//            EUser(
//               id = "1-2-3-4",
//               username = "jxhui",
//               password = "password",
//               trackedDayIds = listOf()
//            )
//         )
//      }
//   }
//
//   @Test
//   fun users() {
//      val userNames: List<String> = dgsQueryExecutor
//         .executeAndExtractJsonPath(
//            """
//         {
//            users {
//               id
//               username
//            }
//         }
//         """.trimIndent(), "data.users[*].username"
//         )
//
//      assertThat(userNames.contains("jxhui"))
//   }
//
//   @Test
//   fun emptyArgs() {
//      val userNames: List<String> = dgsQueryExecutor
//         .executeAndExtractJsonPath(
//            """
//         {
//            users(username: "") {
//               id
//               username
//            }
//         }
//         """.trimIndent(), "data.users[*].username"
//         )
//      assertThat(userNames.isEmpty())
//   }
//
//   @Test
//   fun exceptional() {
//      Mockito.`when`(
//         repository.findAll(
//            ArgumentMatchers.any(
//               BooleanBuilder::class.java
//            )
//         )
//      ).thenThrow(RuntimeException("how exceptional"))
//
//      val result = dgsQueryExecutor.execute(
//         """
//            {
//               users {
//                  id
//               }
//            }
//         """.trimIndent()
//      )
//
//      assertThat(result.errors).isNotEmpty
//      assertThat(result.errors[0].message).isEqualTo(
//         "java.lang" +
//            ".RuntimeException: how exceptional"
//      )
//   }
//}