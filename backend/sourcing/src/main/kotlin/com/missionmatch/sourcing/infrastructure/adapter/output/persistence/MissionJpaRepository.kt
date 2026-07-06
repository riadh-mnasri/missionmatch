package com.missionmatch.sourcing.infrastructure.adapter.output.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MissionJpaRepository : JpaRepository<MissionEntity, UUID>
