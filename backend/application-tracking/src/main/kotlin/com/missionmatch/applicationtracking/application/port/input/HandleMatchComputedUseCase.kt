package com.missionmatch.applicationtracking.application.port.input

import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId

data class MatchComputedCommand(
    val missionId: MissionId,
    val freelancerId: FreelancerId,
    val score: Double,
)

interface HandleMatchComputedUseCase {
    fun handle(command: MatchComputedCommand)
}
