package com.missionmatch.applicationtracking.infrastructure.adapter.output.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CandidatureJpaRepository : JpaRepository<CandidatureEntity, UUID> {
    fun findByFreelancerId(freelancerId: UUID): List<CandidatureEntity>
    fun existsByMissionIdAndFreelancerId(missionId: UUID, freelancerId: UUID): Boolean
}
