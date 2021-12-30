package com.ultish.jikangaaruserver.dataFetchers

import org.junit.jupiter.api.Test
import java.util.*

class DataFetcherUtilsKtTest {

   @Test
   fun testCreatePageableDefaults() {
      val (pageable, pageNumber, size) = createPageable("test")
      assert(pageNumber == 0)
      assert(size == 10)
      assert(pageable.sort.isSorted)
      assert(pageable.sort.get().findFirst().get().property == "test")
      assert(pageable.sort.get().findFirst().get().isAscending)
   }

   @Test
   fun testCreatePageable() {
      val after = Base64.getEncoder().encodeToString("4".toByteArray())
      val (pageable, pageNumber, size) = createPageable(
         "test",
         after = after,
         first = 5
      )
      assert(pageNumber == 1)
      assert(size == 5)
      assert(pageable.hasPrevious())
   }
}