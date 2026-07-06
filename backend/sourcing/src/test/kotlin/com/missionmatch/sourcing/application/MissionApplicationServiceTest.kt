package com.missionmatch.sourcing.application

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import com.missionmatch.sourcing.application.port.input.PublishMissionCommand
import com.missionmatch.sourcing.application.port.output.MissionEventPublisher
import com.missionmatch.sourcing.application.port.output.MissionRepository
import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId
import com.missionmatch.sourcing.domain.event.MissionClosed
import com.missionmatch.sourcing.domain.event.MissionPublished
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class MissionApplicationServiceTest {

    private val missionRepository = mock<MissionRepository>()
    private val missionEventPublisher = mock<MissionEventPublisher>()
    private lateinit var service: MissionApplicationService

    @BeforeEach
    fun setUp() {
        service = MissionApplicationService(missionRepository, missionEventPublisher)
    }

    @Test
    fun `publishing a mission saves it and emits MissionPublished`() {
        // Given
        val command = PublishMissionCommand(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = setOf("kotlin", "spring"),
            dailyRateAmount = BigDecimal.valueOf(600),
            startDate = LocalDate.now().plusWeeks(2),
        )

        // When
        val missionId = service.publish(command)

        // Then
        assertThat(missionId).isNotNull()
        verify(missionRepository).save(any())
        verify(missionEventPublisher).publish(any<MissionPublished>())
    }

    @Test
    fun `closing an existing mission saves it and emits MissionClosed`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now(),
        )
        whenever(missionRepository.findById(mission.id)).thenReturn(mission)

        // When
        service.close(mission.id)

        // Then
        verify(missionRepository).save(mission)
        verify(missionEventPublisher).publish(any<MissionClosed>())
    }

    @Test
    fun `closing an unknown mission fails without emitting an event`() {
        // Given
        val unknownId = MissionId.generate()
        whenever(missionRepository.findById(unknownId)).thenReturn(null)

        // When
        // Then
        assertThatThrownBy { service.close(unknownId) }.isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `getting all missions delegates to the repository`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now(),
        )
        whenever(missionRepository.findAll()).thenReturn(listOf(mission))

        // When
        val missions = service.getAll()

        // Then
        assertThat(missions).containsExactly(mission)
    }
}
