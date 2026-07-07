package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.application.port.output.MatchResultRepository
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult
import com.missionmatch.matching.domain.MissionId
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
class MatchResultRepositoryAdapter(
    private val jpaRepository: MatchResultJpaRepository,
) : MatchResultRepository {

    override fun save(matchResult: MatchResult): MatchResult =
        try {
            jpaRepository.save(MatchResultEntity.fromDomain(matchResult)).toDomain()
        } catch (violation: DataIntegrityViolationException) {
            // Two consumer threads (mission-published and profile-updated) can race to insert
            // the first match for the same (mission, freelancer) pair. The unique constraint on
            // match_results rejects the loser; it just adopts whatever the winner persisted.
            jpaRepository.findByMissionIdAndFreelancerId(matchResult.missionId.value, matchResult.freelancerId.value)
                ?.toDomain()
                ?: throw violation
        }

    override fun findByFreelancerId(freelancerId: FreelancerId): List<MatchResult> =
        jpaRepository.findByFreelancerId(freelancerId.value).map { it.toDomain() }

    override fun findByMissionIdAndFreelancerId(missionId: MissionId, freelancerId: FreelancerId): MatchResult? =
        jpaRepository.findByMissionIdAndFreelancerId(missionId.value, freelancerId.value)?.toDomain()
}
