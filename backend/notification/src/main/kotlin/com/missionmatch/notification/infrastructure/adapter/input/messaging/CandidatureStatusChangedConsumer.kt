package com.missionmatch.notification.infrastructure.adapter.input.messaging

import com.missionmatch.notification.application.port.input.CandidatureStatusChangedNotificationCommand
import com.missionmatch.notification.application.port.input.HandleCandidatureStatusChangedUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class CandidatureStatusChangedConsumer(
    private val handleCandidatureStatusChangedUseCase: HandleCandidatureStatusChangedUseCase,
) {

    @KafkaListener(topics = [CANDIDATURE_STATUS_CHANGED_TOPIC], groupId = "notification")
    fun onCandidatureStatusChanged(event: CandidatureStatusChangedIntegrationEvent) {
        handleCandidatureStatusChangedUseCase.handle(
            CandidatureStatusChangedNotificationCommand(
                candidatureId = event.candidatureId,
                missionId = event.missionId,
                freelancerId = event.freelancerId,
                previousStatus = event.previousStatus,
                newStatus = event.newStatus,
            ),
        )
    }

    companion object {
        const val CANDIDATURE_STATUS_CHANGED_TOPIC = "candidature-status-changed"
    }
}
