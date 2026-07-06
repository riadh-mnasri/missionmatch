package com.missionmatch.matching.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class MatchingPolicyTest {

    private val policy = MatchingPolicy()

    @Test
    fun `perfect skill overlap and an affordable rate score close to 1`() {
        // Given
        val mission = MissionSnapshot(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = SkillSet.of("kotlin", "spring"),
            dailyRate = Money.of(600.0),
            open = true,
        )
        val profile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("kotlin", "spring", "docker"),
            expectedDailyRate = Money.of(550.0),
        )

        // When
        val score = policy.score(mission, profile)

        // Then
        assertThat(score.isAboveThreshold()).isTrue()
        assertThat(score.value).isGreaterThan(0.9)
    }

    @Test
    fun `no skill overlap scores below the eligibility threshold`() {
        // Given
        val mission = MissionSnapshot(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = SkillSet.of("kotlin", "spring"),
            dailyRate = Money.of(600.0),
            open = true,
        )
        val profile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("php", "wordpress"),
            expectedDailyRate = Money.of(400.0),
        )

        // When
        val score = policy.score(mission, profile)

        // Then
        assertThat(score.isAboveThreshold()).isFalse()
    }

    @Test
    fun `a freelancer expecting more than the daily rate lowers the score`() {
        // Given
        val mission = MissionSnapshot(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(400.0),
            open = true,
        )
        val expensiveProfile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("kotlin"),
            expectedDailyRate = Money.of(800.0),
        )
        val affordableProfile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("kotlin"),
            expectedDailyRate = Money.of(400.0),
        )

        // When
        val expensiveScore = policy.score(mission, expensiveProfile)
        val affordableScore = policy.score(mission, affordableProfile)

        // Then
        assertThat(expensiveScore.value).isLessThan(affordableScore.value)
    }

    @Test
    fun `incompatible currencies are treated as no rate compatibility`() {
        // Given
        val mission = MissionSnapshot(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0, "EUR"),
            open = true,
        )
        val profile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("kotlin"),
            expectedDailyRate = Money.of(600.0, "USD"),
        )

        // When
        val score = policy.score(mission, profile)

        // Then
        assertThat(score.value).isEqualTo(0.7 * 1.0 + 0.3 * 0.0)
    }
}
