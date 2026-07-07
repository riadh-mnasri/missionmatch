package com.missionmatch.applicationtracking.infrastructure.adapter.output.persistence

import com.missionmatch.applicationtracking.application.port.output.CandidatureRepository
import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.CandidatureId
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class CandidatureRepositoryAdapter(
    private val jpaRepository: CandidatureJpaRepository,
) : CandidatureRepository {

    override fun save(candidature: Candidature): Candidature =
        try {
            jpaRepository.save(CandidatureEntity.fromDomain(candidature)).toDomain()
        } catch (violation: DataIntegrityViolationException) {
            // Same race as Matching's match_results: two consumer threads can both check
            // existsByMissionIdAndFreelancerId, both see "no candidature yet," and both try to
            // insert. The unique constraint rejects the loser, which adopts the winner's row.
            jpaRepository.findByFreelancerId(candidature.freelancerId.value)
                .map { it.toDomain() }
                .firstOrNull { it.missionId == candidature.missionId }
                ?: throw violation
        }

    override fun findById(candidatureId: CandidatureId): Candidature? =
        jpaRepository.findById(candidatureId.value).map { it.toDomain() }.orElse(null)

    override fun findByFreelancerId(freelancerId: FreelancerId): List<Candidature> =
        jpaRepository.findByFreelancerId(freelancerId.value).map { it.toDomain() }

    override fun existsByMissionIdAndFreelancerId(missionId: MissionId, freelancerId: FreelancerId): Boolean =
        jpaRepository.existsByMissionIdAndFreelancerId(missionId.value, freelancerId.value)
}
