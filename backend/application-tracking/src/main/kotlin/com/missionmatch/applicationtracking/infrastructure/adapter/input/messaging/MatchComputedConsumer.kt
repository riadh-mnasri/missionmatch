package com.missionmatch.applicationtracking.infrastructure.adapter.input.messaging

import com.missionmatch.applicationtracking.application.port.input.HandleMatchComputedUseCase
import com.missionmatch.applicationtracking.application.port.input.MatchComputedCommand
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

// Named explicitly: Spring's default component-scan bean name is the simple class name, and
// Notification also has its own MatchComputedConsumer (same simple name, different package) -
// harmless for the Kotlin compiler, but a bean registry conflict for a single shared Spring
// context. Discovered by actually starting bootstrap, not by reasoning about it in advance.
@Component("applicationTrackingMatchComputedConsumer")
class MatchComputedConsumer(
    private val handleMatchComputedUseCase: HandleMatchComputedUseCase,
) {

    @KafkaListener(
        topics = [MATCH_COMPUTED_TOPIC],
        groupId = "application-tracking",
        containerFactory = "applicationTrackingKafkaListenerContainerFactory",
    )
    fun onMatchComputed(event: MatchComputedIntegrationEvent) {
        handleMatchComputedUseCase.handle(
            MatchComputedCommand(
                missionId = MissionId(event.missionId),
                freelancerId = FreelancerId(event.freelancerId),
                score = event.score,
            ),
        )
    }

    companion object {
        const val MATCH_COMPUTED_TOPIC = "match-computed"
    }
}
