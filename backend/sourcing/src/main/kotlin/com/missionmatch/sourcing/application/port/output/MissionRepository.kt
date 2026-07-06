package com.missionmatch.sourcing.application.port.output

import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId

interface MissionRepository {
    fun save(mission: Mission): Mission
    fun findById(missionId: MissionId): Mission?
    fun findAll(): List<Mission>
}
