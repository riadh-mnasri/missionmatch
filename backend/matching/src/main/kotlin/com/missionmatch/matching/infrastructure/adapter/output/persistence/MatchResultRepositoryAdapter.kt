package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.application.port.output.MatchResultRepository
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult
import org.springframework.stereotype.Component

@Component
class MatchResultRepositoryAdapter(
    private val jpaRepository: MatchResultJpaRepository,
) : MatchResultRepository {

    override fun save(matchResult: MatchResult): MatchResult =
        jpaRepository.save(MatchResultEntity.fromDomain(matchResult)).toDomain()

    override fun findByFreelancerId(freelancerId: FreelancerId): List<MatchResult> =
        jpaRepository.findByFreelancerId(freelancerId.value).map { it.toDomain() }
}
