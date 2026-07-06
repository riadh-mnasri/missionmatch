package com.missionmatch.sourcing.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MissionTest {

    @Test
    fun `a published mission is open and eligible for matching`() {
        // Given
        val skills = SkillSet.of("kotlin", "spring")
        val dailyRate = Money.of(600.0)

        // When
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = skills,
            dailyRate = dailyRate,
            startDate = LocalDate.now().plusWeeks(2),
        )

        // Then
        assertThat(mission.status).isEqualTo(MissionStatus.OPEN)
        assertThat(mission.isEligibleForMatching()).isTrue()
    }

    @Test
    fun `rejects a blank title`() {
        // Given
        val skills = SkillSet.of("kotlin")
        val dailyRate = Money.of(600.0)

        // When
        // Then
        assertThatThrownBy {
            Mission.publish(
                title = "   ",
                clientName = "Acme Corp",
                requiredSkills = skills,
                dailyRate = dailyRate,
                startDate = LocalDate.now(),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `closing an open mission makes it no longer eligible for matching`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now(),
        )

        // When
        mission.close()

        // Then
        assertThat(mission.status).isEqualTo(MissionStatus.CLOSED)
        assertThat(mission.isEligibleForMatching()).isFalse()
    }

    @Test
    fun `closing an already closed mission is rejected`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now(),
        )
        mission.close()

        // When
        // Then
        assertThatThrownBy { mission.close() }.isInstanceOf(IllegalStateException::class.java)
    }
}
