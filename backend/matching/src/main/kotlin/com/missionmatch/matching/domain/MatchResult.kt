package com.missionmatch.matching.domain

import java.time.Instant
import java.util.UUID

data class MatchResultId(val value: UUID) {
    companion object {
        fun generate(): MatchResultId = MatchResultId(UUID.randomUUID())
    }
}

class MatchResult private constructor(
    val id: MatchResultId,
    val missionId: MissionId,
    val freelancerId: FreelancerId,
    val score: MatchingScore,
    val computedAt: Instant,
) {
    fun isEligible(): Boolean = score.isAboveThreshold()

    fun recompute(score: MatchingScore): MatchResult = MatchResult(id, missionId, freelancerId, score, Instant.now())

    companion object {
        fun compute(missionId: MissionId, freelancerId: FreelancerId, score: MatchingScore): MatchResult =
            MatchResult(MatchResultId.generate(), missionId, freelancerId, score, Instant.now())

        fun reconstitute(
            id: MatchResultId,
            missionId: MissionId,
            freelancerId: FreelancerId,
            score: MatchingScore,
            computedAt: Instant,
        ): MatchResult = MatchResult(id, missionId, freelancerId, score, computedAt)
    }
}
