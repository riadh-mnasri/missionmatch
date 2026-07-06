package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult
import com.missionmatch.matching.domain.MatchResultId
import com.missionmatch.matching.domain.MatchingScore
import com.missionmatch.matching.domain.MissionId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "match_results")
class MatchResultEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val missionId: UUID,

    @Column(nullable = false)
    val freelancerId: UUID,

    @Column(nullable = false)
    val score: Double,

    @Column(nullable = false)
    val computedAt: Instant,
) {
    fun toDomain(): MatchResult = MatchResult.reconstitute(
        id = MatchResultId(id),
        missionId = MissionId(missionId),
        freelancerId = FreelancerId(freelancerId),
        score = MatchingScore(score),
        computedAt = computedAt,
    )

    companion object {
        fun fromDomain(matchResult: MatchResult): MatchResultEntity = MatchResultEntity(
            id = matchResult.id.value,
            missionId = matchResult.missionId.value,
            freelancerId = matchResult.freelancerId.value,
            score = matchResult.score.value,
            computedAt = matchResult.computedAt,
        )
    }
}
