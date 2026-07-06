package com.missionmatch.matching.infrastructure.adapter.input.web

import com.missionmatch.matching.application.port.input.GetMatchesUseCase
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult
import com.missionmatch.matching.domain.MatchingScore
import com.missionmatch.matching.domain.MissionId
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(MatchController::class)
class MatchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var getMatchesUseCase: GetMatchesUseCase

    @Test
    fun `returns the matches known for a freelancer`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        val match = MatchResult.compute(MissionId(UUID.randomUUID()), freelancerId, MatchingScore(0.85))
        whenever(getMatchesUseCase.getMatchesForFreelancer(freelancerId)).thenReturn(listOf(match))

        // When
        val result = mockMvc.perform(get("/matches").param("freelancerId", freelancerId.value.toString()))

        // Then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$[0].freelancerId").value(freelancerId.value.toString()))
            .andExpect(jsonPath("$[0].score").value(0.85))
    }
}
