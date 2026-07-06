package com.missionmatch.matching.infrastructure.adapter.input.messaging

import com.missionmatch.matching.application.port.input.HandleMissionPublishedUseCase
import com.missionmatch.matching.application.port.input.MissionPublishedCommand
import com.missionmatch.matching.domain.MissionId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class MissionPublishedConsumer(
    private val handleMissionPublishedUseCase: HandleMissionPublishedUseCase,
) {

    @KafkaListener(topics = [MISSION_PUBLISHED_TOPIC], groupId = "matching")
    fun onMissionPublished(event: MissionPublishedIntegrationEvent) {
        handleMissionPublishedUseCase.handle(
            MissionPublishedCommand(
                missionId = MissionId(event.missionId),
                requiredSkills = event.requiredSkills,
                dailyRateAmount = event.dailyRateAmount,
                dailyRateCurrency = event.dailyRateCurrency,
            ),
        )
    }

    companion object {
        const val MISSION_PUBLISHED_TOPIC = "mission-published"
    }
}
