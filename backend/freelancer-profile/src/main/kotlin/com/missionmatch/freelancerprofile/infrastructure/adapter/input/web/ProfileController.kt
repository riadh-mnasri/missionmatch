package com.missionmatch.freelancerprofile.infrastructure.adapter.input.web

import com.missionmatch.freelancerprofile.application.port.input.GetProfileUseCase
import com.missionmatch.freelancerprofile.application.port.input.UpdateProfileCommand
import com.missionmatch.freelancerprofile.application.port.input.UpdateProfileUseCase
import com.missionmatch.freelancerprofile.domain.FreelancerId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/profile")
class ProfileController(
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
) {

    @PutMapping("/{freelancerId}")
    fun update(@PathVariable freelancerId: UUID, @RequestBody request: UpdateProfileRequest): ProfileResponse {
        updateProfileUseCase.update(
            UpdateProfileCommand(
                freelancerId = FreelancerId(freelancerId),
                skills = request.skills,
                expectedDailyRateAmount = request.expectedDailyRateAmount,
                expectedDailyRateCurrency = request.expectedDailyRateCurrency,
            ),
        )
        val profile = getProfileUseCase.getById(FreelancerId(freelancerId))
            ?: error("Profile $freelancerId was just updated but could not be reloaded")

        return ProfileResponse.from(profile)
    }

    @GetMapping("/{freelancerId}")
    fun getById(@PathVariable freelancerId: UUID): ResponseEntity<ProfileResponse> {
        val profile = getProfileUseCase.getById(FreelancerId(freelancerId)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(ProfileResponse.from(profile))
    }
}
