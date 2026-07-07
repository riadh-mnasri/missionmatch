package com.missionmatch.applicationtracking.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class CandidatureTest {

    @Test
    fun `a suggested candidature starts in TO_APPLY`() {
        // Given
        val missionId = MissionId(UUID.randomUUID())
        val freelancerId = FreelancerId(UUID.randomUUID())

        // When
        val candidature = Candidature.suggest(missionId, freelancerId)

        // Then
        assertThat(candidature.status).isEqualTo(CandidatureStatus.TO_APPLY)
    }

    @Test
    fun `a candidature can move from TO_APPLY to APPLIED to INTERVIEW to ACCEPTED`() {
        // Given
        val candidature = Candidature.suggest(MissionId(UUID.randomUUID()), FreelancerId(UUID.randomUUID()))

        // When
        candidature.moveTo(CandidatureStatus.APPLIED)
        candidature.moveTo(CandidatureStatus.INTERVIEW)
        candidature.moveTo(CandidatureStatus.ACCEPTED)

        // Then
        assertThat(candidature.status).isEqualTo(CandidatureStatus.ACCEPTED)
    }

    @Test
    fun `a candidature can be rejected after an interview`() {
        // Given
        val candidature = Candidature.suggest(MissionId(UUID.randomUUID()), FreelancerId(UUID.randomUUID()))
        candidature.moveTo(CandidatureStatus.APPLIED)
        candidature.moveTo(CandidatureStatus.INTERVIEW)

        // When
        candidature.moveTo(CandidatureStatus.REJECTED)

        // Then
        assertThat(candidature.status).isEqualTo(CandidatureStatus.REJECTED)
    }

    @Test
    fun `a candidature cannot skip straight from TO_APPLY to INTERVIEW`() {
        // Given
        val candidature = Candidature.suggest(MissionId(UUID.randomUUID()), FreelancerId(UUID.randomUUID()))

        // When
        // Then
        assertThatThrownBy { candidature.moveTo(CandidatureStatus.INTERVIEW) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a rejected candidature is terminal`() {
        // Given
        val candidature = Candidature.suggest(MissionId(UUID.randomUUID()), FreelancerId(UUID.randomUUID()))
        candidature.moveTo(CandidatureStatus.APPLIED)
        candidature.moveTo(CandidatureStatus.REJECTED)

        // When
        // Then
        assertThatThrownBy { candidature.moveTo(CandidatureStatus.APPLIED) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
