package com.missionmatch.applicationtracking.application.port.input

import com.missionmatch.applicationtracking.domain.CandidatureId
import com.missionmatch.applicationtracking.domain.CandidatureStatus

data class MoveCandidatureCommand(
    val candidatureId: CandidatureId,
    val newStatus: CandidatureStatus,
)

interface MoveCandidatureUseCase {
    fun move(command: MoveCandidatureCommand)
}
