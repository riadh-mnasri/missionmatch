package com.missionmatch.sourcing.application.port.input

import com.missionmatch.sourcing.domain.MissionId

interface CloseMissionUseCase {
    fun close(missionId: MissionId)
}
