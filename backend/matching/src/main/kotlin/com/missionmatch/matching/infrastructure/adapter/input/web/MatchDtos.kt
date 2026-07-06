package com.missionmatch.matching.infrastructure.adapter.input.web

import com.missionmatch.matching.domain.MatchResult
import java.time.Instant
import java.util.UUID

data class MatchResponse(
    val missionId: UUID,
    val freelancerId: UUID,
    val score: Double,
    val computedAt: Instant,
) {
    companion object {
        fun from(matchResult: MatchResult): MatchResponse = MatchResponse(
            missionId = matchResult.missionId.value,
            freelancerId = matchResult.freelancerId.value,
            score = matchResult.score.value,
            computedAt = matchResult.computedAt,
        )
    }
}
