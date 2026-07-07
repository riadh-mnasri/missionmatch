package com.missionmatch.notification.infrastructure.adapter.input.messaging

import com.missionmatch.notification.application.port.input.HandleMatchComputedUseCase
import com.missionmatch.notification.application.port.input.MatchComputedNotificationCommand
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

// Named explicitly: see ApplicationTracking's MatchComputedConsumer for why (a Spring bean
// registry name conflict, not a Kotlin compile error).
@Component("notificationMatchComputedConsumer")
class MatchComputedConsumer(
    private val handleMatchComputedUseCase: HandleMatchComputedUseCase,
) {

    @KafkaListener(
        topics = [MATCH_COMPUTED_TOPIC],
        groupId = "notification",
        containerFactory = "notificationKafkaListenerContainerFactory",
    )
    fun onMatchComputed(event: MatchComputedIntegrationEvent) {
        handleMatchComputedUseCase.handle(
            MatchComputedNotificationCommand(
                missionId = event.missionId,
                freelancerId = event.freelancerId,
                score = event.score,
            ),
        )
    }

    companion object {
        const val MATCH_COMPUTED_TOPIC = "match-computed"
    }
}
