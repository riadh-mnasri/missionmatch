package com.missionmatch.applicationtracking.infrastructure.adapter.input.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.missionmatch.applicationtracking.application.port.input.GetCandidaturesUseCase
import com.missionmatch.applicationtracking.application.port.input.MoveCandidatureUseCase
import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.CandidatureStatus
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(CandidatureController::class)
class CandidatureControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var getCandidaturesUseCase: GetCandidaturesUseCase

    @MockBean
    private lateinit var moveCandidatureUseCase: MoveCandidatureUseCase

    @Test
    fun `lists candidatures for a freelancer`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        val candidature = Candidature.suggest(MissionId(UUID.randomUUID()), freelancerId)
        whenever(getCandidaturesUseCase.getForFreelancer(freelancerId)).thenReturn(listOf(candidature))

        // When
        val result = mockMvc.perform(get("/api/candidatures").param("freelancerId", freelancerId.value.toString()))

        // Then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$[0].status").value("TO_APPLY"))
    }

    @Test
    fun `updates a candidature's status`() {
        // Given
        val candidatureId = UUID.randomUUID()
        val request = UpdateCandidatureStatusRequest(status = CandidatureStatus.APPLIED)

        // When
        val result = mockMvc.perform(
            patch("/api/candidatures/$candidatureId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        // Then
        result.andExpect(status().isOk)
        verify(moveCandidatureUseCase).move(any())
    }
}
