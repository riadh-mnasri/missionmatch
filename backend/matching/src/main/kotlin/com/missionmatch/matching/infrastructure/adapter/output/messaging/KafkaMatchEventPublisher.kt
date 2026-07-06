package com.missionmatch.matching.infrastructure.adapter.output.messaging

import com.missionmatch.matching.application.port.output.MatchEventPublisher
import com.missionmatch.matching.domain.event.MatchComputed
import com.missionmatch.shared.event.DomainEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaMatchEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : MatchEventPublisher {

    override fun publish(event: DomainEvent) {
        when (event) {
            is MatchComputed -> kafkaTemplate.send(MATCH_COMPUTED_TOPIC, event.missionId.value.toString(), event)
            else -> error("No topic configured for event type ${event::class.simpleName}")
        }
    }

    companion object {
        const val MATCH_COMPUTED_TOPIC = "match-computed"
    }
}
