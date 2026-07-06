package com.missionmatch.shared.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SkillSetTest {

    @Test
    fun `normalizes skills to trimmed lowercase`() {
        // Given
        val rawSkills = listOf(" Kotlin", "SPRING ", "kafka")

        // When
        val skillSet = SkillSet.of(rawSkills)

        // Then
        assertThat(skillSet.skills).containsExactlyInAnyOrder("kotlin", "spring", "kafka")
    }

    @Test
    fun `rejects an empty skill set`() {
        // Given
        val noSkills = emptySet<String>()

        // When
        // Then
        assertThatThrownBy { SkillSet(noSkills) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `computes the overlap ratio against another skill set`() {
        // Given
        val required = SkillSet.of("kotlin", "spring", "kafka")
        val profile = SkillSet.of("kotlin", "spring", "docker")

        // When
        val ratio = required.overlapRatioWith(profile)

        // Then
        assertThat(ratio).isEqualTo(2.0 / 3.0)
    }
}
