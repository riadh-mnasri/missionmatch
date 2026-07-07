package com.missionmatch.matching.infrastructure.adapter.input.messaging

import com.missionmatch.matching.application.port.input.HandleMissionClosedUseCase
import com.missionmatch.matching.application.port.input.MissionClosedCommand
import com.missionmatch.matching.domain.MissionId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class MissionClosedConsumer(
    private val handleMissionClosedUseCase: HandleMissionClosedUseCase,
) {

    @KafkaListener(topics = [MISSION_CLOSED_TOPIC], groupId = "matching")
    fun onMissionClosed(event: MissionClosedIntegrationEvent) {
        handleMissionClosedUseCase.handle(MissionClosedCommand(MissionId(event.missionId)))
    }

    companion object {
        const val MISSION_CLOSED_TOPIC = "mission-closed"
    }
}
