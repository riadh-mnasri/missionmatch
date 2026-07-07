package com.missionmatch.matching.application.port.input

import com.missionmatch.matching.domain.MissionId

data class MissionClosedCommand(val missionId: MissionId)

interface HandleMissionClosedUseCase {
    fun handle(command: MissionClosedCommand)
}
