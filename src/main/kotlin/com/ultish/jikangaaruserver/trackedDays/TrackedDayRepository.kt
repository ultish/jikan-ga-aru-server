package com.ultish.jikangaaruserver.trackedDays

import com.ultish.jikangaaruserver.entities.ETrackedDay
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface TrackedDayRepository : MongoRepository<ETrackedDay, String>,
    QuerydslPredicateExecutor<ETrackedDay> {


    @Query("{ \$expr: { \$eq: [{ \$month: '\$date' }, ?0] }}")
    fun findByMonth(month: Int): List<ETrackedDay>

    @Query(
        """{ 
           ${'$'}expr: { 
              ${'$'}and: [
                 { ${'$'}eq: [{ ${'$'}month: '${'$'}date' }, :#{#month}] },
                 { ${'$'}eq: [{ ${'$'}year: '${'$'}date' }, :#{#year}] },
                 { ${'$'}eq: ['${'$'}userId', :#{#userId}] }
              ]
           }
        }"""
    )
    fun findByMonthAndYear(month: Int, year: Int, userId: String): List<ETrackedDay>

}