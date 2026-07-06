package com.missionmatch.matching.infrastructure.adapter.output.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MatchResultJpaRepository : JpaRepository<MatchResultEntity, UUID> {
    fun findByFreelancerId(freelancerId: UUID): List<MatchResultEntity>
}
