package com.missionmatch.sourcing.infrastructure.adapter.output.messaging

import com.missionmatch.shared.event.DomainEvent
import com.missionmatch.sourcing.application.port.output.MissionEventPublisher
import com.missionmatch.sourcing.domain.event.MissionClosed
import com.missionmatch.sourcing.domain.event.MissionPublished
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaMissionEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MissionEventPublisher {

    override fun publish(event: DomainEvent) {
        val topic = topicFor(event)
        val key = keyFor(event)
        kafkaTemplate.send(topic, key, event)
    }

    private fun topicFor(event: DomainEvent): String = when (event) {
        is MissionPublished -> MISSION_PUBLISHED_TOPIC
        is MissionClosed -> MISSION_CLOSED_TOPIC
        else -> error("No topic configured for event type ${event::class.simpleName}")
    }

    private fun keyFor(event: DomainEvent): String = when (event) {
        is MissionPublished -> event.missionId.value.toString()
        is MissionClosed -> event.missionId.value.toString()
        else -> event.metadata.eventId.toString()
    }

    companion object {
        const val MISSION_PUBLISHED_TOPIC = "mission-published"
        const val MISSION_CLOSED_TOPIC = "mission-closed"
    }
}
