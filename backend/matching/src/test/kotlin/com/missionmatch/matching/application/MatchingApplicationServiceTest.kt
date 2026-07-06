package com.missionmatch.matching.application

import com.missionmatch.matching.application.port.input.MissionPublishedCommand
import com.missionmatch.matching.application.port.input.ProfileUpdatedCommand
import com.missionmatch.matching.application.port.output.MatchEventPublisher
import com.missionmatch.matching.application.port.output.MatchResultRepository
import com.missionmatch.matching.application.port.output.MissionSnapshotRepository
import com.missionmatch.matching.application.port.output.ProfileSnapshotRepository
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchingPolicy
import com.missionmatch.matching.domain.MissionId
import com.missionmatch.matching.domain.MissionSnapshot
import com.missionmatch.matching.domain.ProfileSnapshot
import com.missionmatch.matching.domain.event.MatchComputed
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.UUID

class MatchingApplicationServiceTest {

    private val missionSnapshotRepository = mock<MissionSnapshotRepository>()
    private val profileSnapshotRepository = mock<ProfileSnapshotRepository>()
    private val matchResultRepository = mock<MatchResultRepository>()
    private val matchEventPublisher = mock<MatchEventPublisher>()
    private lateinit var service: MatchingApplicationService

    @BeforeEach
    fun setUp() {
        service = MatchingApplicationService(
            missionSnapshotRepository,
            profileSnapshotRepository,
            matchResultRepository,
            matchEventPublisher,
            MatchingPolicy(),
        )
    }

    @Test
    fun `a published mission is scored against every known profile and eligible matches are published`() {
        // Given
        val matchingProfile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("kotlin", "spring"),
            expectedDailyRate = Money.of(500.0),
        )
        val nonMatchingProfile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("php"),
            expectedDailyRate = Money.of(500.0),
        )
        whenever(profileSnapshotRepository.findAll()).thenReturn(listOf(matchingProfile, nonMatchingProfile))

        val command = MissionPublishedCommand(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = setOf("kotlin", "spring"),
            dailyRateAmount = BigDecimal.valueOf(600),
            dailyRateCurrency = "EUR",
        )

        // When
        service.handle(command)

        // Then
        verify(missionSnapshotRepository).save(any())
        verify(matchResultRepository).save(any())
        verify(matchEventPublisher).publish(any<MatchComputed>())
    }

    @Test
    fun `an updated profile is scored against every known open mission`() {
        // Given
        val openMission = MissionSnapshot(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0),
            open = true,
        )
        whenever(missionSnapshotRepository.findAllOpen()).thenReturn(listOf(openMission))

        val command = ProfileUpdatedCommand(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = setOf("kotlin"),
            expectedDailyRateAmount = BigDecimal.valueOf(500),
            expectedDailyRateCurrency = "EUR",
        )

        // When
        service.handle(command)

        // Then
        verify(profileSnapshotRepository).save(any())
        verify(matchResultRepository).save(any())
        verify(matchEventPublisher).publish(any<MatchComputed>())
    }

    @Test
    fun `a below-threshold score is neither persisted nor published`() {
        // Given
        val nonMatchingProfile = ProfileSnapshot(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("php"),
            expectedDailyRate = Money.of(500.0),
        )
        whenever(profileSnapshotRepository.findAll()).thenReturn(listOf(nonMatchingProfile))

        val command = MissionPublishedCommand(
            missionId = MissionId(UUID.randomUUID()),
            requiredSkills = setOf("kotlin", "spring"),
            dailyRateAmount = BigDecimal.valueOf(600),
            dailyRateCurrency = "EUR",
        )

        // When
        service.handle(command)

        // Then
        verify(matchResultRepository, never()).save(any())
        verify(matchEventPublisher, never()).publish(any())
    }

    @Test
    fun `matches for a freelancer are delegated to the repository`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        whenever(matchResultRepository.findByFreelancerId(freelancerId)).thenReturn(emptyList())

        // When
        val matches = service.getMatchesForFreelancer(freelancerId)

        // Then
        assertThat(matches).isEmpty()
    }
}
