package com.missionmatch.freelancerprofile.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ProfileTest {

    @Test
    fun `two profiles with the same values are equal`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        val skills = SkillSet.of("kotlin", "spring")
        val expectedDailyRate = Money.of(550.0)

        // When
        val first = Profile(freelancerId, skills, expectedDailyRate)
        val second = Profile(freelancerId, skills, expectedDailyRate)

        // Then
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `updating a profile replaces its skills and expected rate`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        val original = Profile(freelancerId, SkillSet.of("kotlin"), Money.of(500.0))

        // When
        val updated = original.copy(skills = SkillSet.of("kotlin", "kafka"), expectedDailyRate = Money.of(600.0))

        // Then
        assertThat(updated.freelancerId).isEqualTo(freelancerId)
        assertThat(updated.skills.skills).containsExactlyInAnyOrder("kotlin", "kafka")
        assertThat(updated.expectedDailyRate.amount).isEqualByComparingTo("600.0")
    }
}
