package com.missionmatch.matching.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class MatchResultTest {

    @Test
    fun `a match with a score above the threshold is eligible`() {
        // Given
        val missionId = MissionId(UUID.randomUUID())
        val freelancerId = FreelancerId(UUID.randomUUID())
        val score = MatchingScore(0.8)

        // When
        val match = MatchResult.compute(missionId, freelancerId, score)

        // Then
        assertThat(match.isEligible()).isTrue()
    }

    @Test
    fun `a match with a score below the threshold is not eligible`() {
        // Given
        val missionId = MissionId(UUID.randomUUID())
        val freelancerId = FreelancerId(UUID.randomUUID())
        val score = MatchingScore(0.2)

        // When
        val match = MatchResult.compute(missionId, freelancerId, score)

        // Then
        assertThat(match.isEligible()).isFalse()
    }
}
