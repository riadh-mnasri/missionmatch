package com.missionmatch.notification.application.port.input

import java.util.UUID

data class MatchComputedNotificationCommand(
    val missionId: UUID,
    val freelancerId: UUID,
    val score: Double,
)

interface HandleMatchComputedUseCase {
    fun handle(command: MatchComputedNotificationCommand)
}
