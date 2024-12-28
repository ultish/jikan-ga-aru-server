package com.ultish.jikangaaruserver.listeners

import com.ultish.jikangaaruserver.entities.GraphQLEntity
import org.springframework.data.mongodb.core.mapping.event.AfterDeleteEvent
import org.springframework.data.mongodb.core.mapping.event.AfterSaveEvent

fun <T> getIdFrom(event: AfterDeleteEvent<T>): String? {
    return event.source["_id"]?.toString()
}

fun <G, T : GraphQLEntity<G>> getIdFrom(event: AfterSaveEvent<T>): String {
    return event.source.id()
}