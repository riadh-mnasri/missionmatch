package com.missionmatch.sourcing.infrastructure.adapter.input.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import com.missionmatch.sourcing.application.port.input.CloseMissionUseCase
import com.missionmatch.sourcing.application.port.input.GetMissionUseCase
import com.missionmatch.sourcing.application.port.input.PublishMissionUseCase
import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(MissionController::class)
class MissionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var publishMissionUseCase: PublishMissionUseCase

    @MockBean
    private lateinit var closeMissionUseCase: CloseMissionUseCase

    @MockBean
    private lateinit var getMissionUseCase: GetMissionUseCase

    @Test
    fun `publishing a mission returns 201 with the created mission`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin", "spring"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now().plusWeeks(2),
        )
        whenever(publishMissionUseCase.publish(any())).thenReturn(mission.id)
        whenever(getMissionUseCase.getById(mission.id)).thenReturn(mission)

        val request = PublishMissionRequest(
            title = mission.title,
            clientName = mission.clientName,
            requiredSkills = mission.requiredSkills.skills,
            dailyRateAmount = mission.dailyRate.amount,
            startDate = mission.startDate,
        )

        // When
        val result = mockMvc.perform(
            post("/api/missions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        // Then
        result.andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value(mission.title))
            .andExpect(jsonPath("$.status").value("OPEN"))
    }

    @Test
    fun `listing missions returns every known mission`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now(),
        )
        whenever(getMissionUseCase.getAll()).thenReturn(listOf(mission))

        // When
        val result = mockMvc.perform(get("/api/missions"))

        // Then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$[0].title").value(mission.title))
    }

    @Test
    fun `fetching an unknown mission returns 404`() {
        // Given
        val unknownId = MissionId.generate()
        whenever(getMissionUseCase.getById(unknownId)).thenReturn(null)

        // When
        val result = mockMvc.perform(get("/api/missions/${unknownId.value}"))

        // Then
        result.andExpect(status().isNotFound)
    }

    @Test
    fun `closing a mission returns 204`() {
        // Given
        val missionId = MissionId.generate()

        // When
        val result = mockMvc.perform(post("/api/missions/${missionId.value}/close"))

        // Then
        result.andExpect(status().isNoContent)
        verify(closeMissionUseCase).close(missionId)
    }
}
