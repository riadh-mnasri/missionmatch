package com.missionmatch.freelancerprofile.infrastructure.adapter.output.messaging

import com.missionmatch.freelancerprofile.application.port.output.ProfileEventPublisher
import com.missionmatch.freelancerprofile.domain.event.ProfileUpdated
import com.missionmatch.shared.event.DomainEvent
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaProfileEventPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : ProfileEventPublisher {

    override fun publish(event: DomainEvent) {
        when (event) {
            is ProfileUpdated -> kafkaTemplate.send(PROFILE_UPDATED_TOPIC, event.freelancerId.value.toString(), event)
            else -> error("No topic configured for event type ${event::class.simpleName}")
        }
    }

    companion object {
        const val PROFILE_UPDATED_TOPIC = "profile-updated"
    }
}
