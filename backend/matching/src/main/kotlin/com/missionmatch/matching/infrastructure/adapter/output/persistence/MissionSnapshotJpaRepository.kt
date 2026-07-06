package com.missionmatch.matching.infrastructure.adapter.output.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MissionSnapshotJpaRepository : JpaRepository<MissionSnapshotEntity, UUID> {
    fun findByOpenTrue(): List<MissionSnapshotEntity>
}
