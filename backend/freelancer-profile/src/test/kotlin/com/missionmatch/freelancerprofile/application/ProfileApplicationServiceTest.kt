package com.missionmatch.freelancerprofile.application

import com.missionmatch.freelancerprofile.application.port.input.UpdateProfileCommand
import com.missionmatch.freelancerprofile.application.port.output.ProfileEventPublisher
import com.missionmatch.freelancerprofile.application.port.output.ProfileRepository
import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile
import com.missionmatch.freelancerprofile.domain.event.ProfileUpdated
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.UUID

class ProfileApplicationServiceTest {

    private val profileRepository = mock<ProfileRepository>()
    private val profileEventPublisher = mock<ProfileEventPublisher>()
    private lateinit var service: ProfileApplicationService

    @BeforeEach
    fun setUp() {
        service = ProfileApplicationService(profileRepository, profileEventPublisher)
    }

    @Test
    fun `updating a profile saves it and emits ProfileUpdated`() {
        // Given
        val command = UpdateProfileCommand(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = setOf("kotlin", "spring"),
            expectedDailyRateAmount = BigDecimal.valueOf(550),
            expectedDailyRateCurrency = "EUR",
        )

        // When
        service.update(command)

        // Then
        verify(profileRepository).save(any())
        verify(profileEventPublisher).publish(any<ProfileUpdated>())
    }

    @Test
    fun `getting a profile delegates to the repository`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        val profile = Profile(freelancerId, SkillSet.of("kotlin"), Money.of(500.0))
        whenever(profileRepository.findById(freelancerId)).thenReturn(profile)

        // When
        val found = service.getById(freelancerId)

        // Then
        assertThat(found).isEqualTo(profile)
    }
}
