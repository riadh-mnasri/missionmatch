package com.missionmatch.freelancerprofile.infrastructure.adapter.input.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.missionmatch.freelancerprofile.application.port.input.GetProfileUseCase
import com.missionmatch.freelancerprofile.application.port.input.UpdateProfileUseCase
import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

@WebMvcTest(ProfileController::class)
class ProfileControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var updateProfileUseCase: UpdateProfileUseCase

    @MockBean
    private lateinit var getProfileUseCase: GetProfileUseCase

    @Test
    fun `updating a profile returns the saved profile`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        val profile = Profile(freelancerId, SkillSet.of("kotlin", "spring"), Money.of(550.0))
        whenever(getProfileUseCase.getById(freelancerId)).thenReturn(profile)

        val request = UpdateProfileRequest(
            skills = setOf("kotlin", "spring"),
            expectedDailyRateAmount = BigDecimal.valueOf(550),
        )

        // When
        val result = mockMvc.perform(
            put("/api/profile/${freelancerId.value}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )

        // Then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.freelancerId").value(freelancerId.value.toString()))
        verify(updateProfileUseCase).update(any())
    }

    @Test
    fun `fetching an unknown profile returns 404`() {
        // Given
        val unknownId = UUID.randomUUID()
        whenever(getProfileUseCase.getById(FreelancerId(unknownId))).thenReturn(null)

        // When
        val result = mockMvc.perform(get("/api/profile/$unknownId"))

        // Then
        result.andExpect(status().isNotFound)
    }
}
