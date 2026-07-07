package com.missionmatch.applicationtracking.infrastructure.adapter.output.messaging

import com.missionmatch.applicationtracking.application.port.output.CandidatureEventPublisher
import com.missionmatch.applicationtracking.domain.event.CandidatureStatusChanged
import com.missionmatch.shared.event.DomainEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaCandidatureEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : CandidatureEventPublisher {

    override fun publish(event: DomainEvent) {
        when (event) {
            is CandidatureStatusChanged ->
                kafkaTemplate.send(CANDIDATURE_STATUS_CHANGED_TOPIC, event.candidatureId.value.toString(), event)
            else -> error("No topic configured for event type ${event::class.simpleName}")
        }
    }

    companion object {
        const val CANDIDATURE_STATUS_CHANGED_TOPIC = "candidature-status-changed"
    }
}
