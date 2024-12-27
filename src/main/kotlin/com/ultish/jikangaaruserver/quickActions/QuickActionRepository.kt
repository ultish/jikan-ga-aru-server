package com.ultish.jikangaaruserver.quickActions;

import com.ultish.jikangaaruserver.entities.EQuickAction;
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor

interface QuickActionRepository : MongoRepository<EQuickAction, String>, QuerydslPredicateExecutor<EQuickAction> {

}
