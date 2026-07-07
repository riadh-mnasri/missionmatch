package com.missionmatch.applicationtracking.application

import com.missionmatch.applicationtracking.application.port.input.MatchComputedCommand
import com.missionmatch.applicationtracking.application.port.input.MoveCandidatureCommand
import com.missionmatch.applicationtracking.application.port.output.CandidatureEventPublisher
import com.missionmatch.applicationtracking.application.port.output.CandidatureRepository
import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.CandidatureStatus
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
import com.missionmatch.applicationtracking.domain.event.CandidatureStatusChanged
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ApplicationTrackingServiceTest {

    private val candidatureRepository = mock<CandidatureRepository>()
    private val candidatureEventPublisher = mock<CandidatureEventPublisher>()
    private lateinit var service: ApplicationTrackingService

    @BeforeEach
    fun setUp() {
        service = ApplicationTrackingService(candidatureRepository, candidatureEventPublisher)
    }

    @Test
    fun `a computed match suggests a new candidature in TO_APPLY`() {
        // Given
        val missionId = MissionId(UUID.randomUUID())
        val freelancerId = FreelancerId(UUID.randomUUID())
        whenever(candidatureRepository.existsByMissionIdAndFreelancerId(missionId, freelancerId)).thenReturn(false)

        // When
        service.handle(MatchComputedCommand(missionId, freelancerId, score = 0.85))

        // Then
        val savedCaptor = argumentCaptor<Candidature>()
        verify(candidatureRepository).save(savedCaptor.capture())
        assertThat(savedCaptor.firstValue.status).isEqualTo(CandidatureStatus.TO_APPLY)
        verify(candidatureEventPublisher).publish(any<CandidatureStatusChanged>())
    }

    @Test
    fun `a redelivered match-computed event does not suggest a duplicate candidature`() {
        // Given
        val missionId = MissionId(UUID.randomUUID())
        val freelancerId = FreelancerId(UUID.randomUUID())
        whenever(candidatureRepository.existsByMissionIdAndFreelancerId(missionId, freelancerId)).thenReturn(true)

        // When
        service.handle(MatchComputedCommand(missionId, freelancerId, score = 0.85))

        // Then
        verify(candidatureRepository, never()).save(any())
        verify(candidatureEventPublisher, never()).publish(any())
    }

    @Test
    fun `moving a candidature persists the new status and publishes the change`() {
        // Given
        val candidature = Candidature.suggest(MissionId(UUID.randomUUID()), FreelancerId(UUID.randomUUID()))
        whenever(candidatureRepository.findById(candidature.id)).thenReturn(candidature)

        // When
        service.move(MoveCandidatureCommand(candidature.id, CandidatureStatus.APPLIED))

        // Then
        verify(candidatureRepository).save(candidature)
        val eventCaptor = argumentCaptor<CandidatureStatusChanged>()
        verify(candidatureEventPublisher).publish(eventCaptor.capture())
        assertThat(eventCaptor.firstValue.previousStatus).isEqualTo(CandidatureStatus.TO_APPLY)
        assertThat(eventCaptor.firstValue.newStatus).isEqualTo(CandidatureStatus.APPLIED)
    }

    @Test
    fun `moving an unknown candidature fails`() {
        // Given
        val unknownId = Candidature.suggest(MissionId(UUID.randomUUID()), FreelancerId(UUID.randomUUID())).id
        whenever(candidatureRepository.findById(unknownId)).thenReturn(null)

        // When
        // Then
        assertThatThrownBy {
            service.move(MoveCandidatureCommand(unknownId, CandidatureStatus.APPLIED))
        }.isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `candidatures for a freelancer are delegated to the repository`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        whenever(candidatureRepository.findByFreelancerId(freelancerId)).thenReturn(emptyList())

        // When
        val result = service.getForFreelancer(freelancerId)

        // Then
        assertThat(result).isEmpty()
    }
}
