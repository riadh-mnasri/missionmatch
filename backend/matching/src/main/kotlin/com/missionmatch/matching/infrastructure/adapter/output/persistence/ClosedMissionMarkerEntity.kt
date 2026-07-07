package com.missionmatch.matching.infrastructure.adapter.output.persistence

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "closed_mission_markers")
class ClosedMissionMarkerEntity(
    @Id
    val missionId: UUID,
)
