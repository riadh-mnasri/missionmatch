package com.missionmatch.notification.application.port.input

import java.util.UUID

data class CandidatureStatusChangedNotificationCommand(
    val candidatureId: UUID,
    val missionId: UUID,
    val freelancerId: UUID,
    val previousStatus: String?,
    val newStatus: String,
)

interface HandleCandidatureStatusChangedUseCase {
    fun handle(command: CandidatureStatusChangedNotificationCommand)
}
