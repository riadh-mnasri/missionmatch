package com.missionmatch.applicationtracking.infrastructure.adapter.input.messaging

import com.missionmatch.applicationtracking.application.port.input.HandleMatchComputedUseCase
import com.missionmatch.applicationtracking.application.port.input.MatchComputedCommand
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class MatchComputedConsumer(
    private val handleMatchComputedUseCase: HandleMatchComputedUseCase,
) {

    @KafkaListener(topics = [MATCH_COMPUTED_TOPIC], groupId = "application-tracking")
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
