package com.missionmatch.sourcing.application.port.input

import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId

interface GetMissionUseCase {
    fun getById(missionId: MissionId): Mission?
    fun getAll(): List<Mission>
}
