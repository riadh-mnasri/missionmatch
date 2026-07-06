package com.missionmatch.matching.application.port.output

import com.missionmatch.matching.domain.MissionId
import com.missionmatch.matching.domain.MissionSnapshot

interface MissionSnapshotRepository {
    fun save(snapshot: MissionSnapshot): MissionSnapshot
    fun findById(missionId: MissionId): MissionSnapshot?
    fun findAllOpen(): List<MissionSnapshot>
}
