package com.missionmatch.freelancerprofile.application.port.input

import com.missionmatch.freelancerprofile.domain.FreelancerId
import java.math.BigDecimal

data class UpdateProfileCommand(
    val freelancerId: FreelancerId,
    val skills: Set<String>,
    val expectedDailyRateAmount: BigDecimal,
    val expectedDailyRateCurrency: String,
)

interface UpdateProfileUseCase {
    fun update(command: UpdateProfileCommand)
}
