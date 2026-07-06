package com.missionmatch.matching.application.port.input

import com.missionmatch.matching.domain.FreelancerId
import java.math.BigDecimal

data class ProfileUpdatedCommand(
    val freelancerId: FreelancerId,
    val skills: Set<String>,
    val expectedDailyRateAmount: BigDecimal,
    val expectedDailyRateCurrency: String,
)

interface HandleProfileUpdatedUseCase {
    fun handle(command: ProfileUpdatedCommand)
}
