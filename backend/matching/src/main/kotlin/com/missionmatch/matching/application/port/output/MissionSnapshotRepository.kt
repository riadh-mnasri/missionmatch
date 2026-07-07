package com.missionmatch.matching.application.port.output

import com.missionmatch.matching.domain.MissionId
import com.missionmatch.matching.domain.MissionSnapshot

interface MissionSnapshotRepository {
    fun save(snapshot: MissionSnapshot): MissionSnapshot
    fun findById(missionId: MissionId): MissionSnapshot?
    fun findAllOpen(): List<MissionSnapshot>

    // MissionClosed can arrive before MissionPublished has created a snapshot to close (they are
    // different Kafka topics, so there is no ordering guarantee between them). These record the
    // fact "this mission is closed" independently of whether a snapshot exists yet.
    fun markClosed(missionId: MissionId)
    fun isMarkedClosed(missionId: MissionId): Boolean
}
