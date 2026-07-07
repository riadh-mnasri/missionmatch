package com.missionmatch.freelancerprofile.infrastructure.adapter.input.web

import com.missionmatch.freelancerprofile.domain.Profile
import java.math.BigDecimal
import java.util.UUID

data class UpdateProfileRequest(
    val skills: Set<String>,
    val expectedDailyRateAmount: BigDecimal,
    val expectedDailyRateCurrency: String = "EUR",
)

data class ProfileResponse(
    val freelancerId: UUID,
    val skills: Set<String>,
    val expectedDailyRateAmount: BigDecimal,
    val expectedDailyRateCurrency: String,
) {
    companion object {
        fun from(profile: Profile): ProfileResponse = ProfileResponse(
            freelancerId = profile.freelancerId.value,
            skills = profile.skills.skills,
            expectedDailyRateAmount = profile.expectedDailyRate.amount,
            expectedDailyRateCurrency = profile.expectedDailyRate.currency,
        )
    }
}
