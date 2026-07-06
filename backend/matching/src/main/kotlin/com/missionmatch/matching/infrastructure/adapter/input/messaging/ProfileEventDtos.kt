package com.missionmatch.matching.infrastructure.adapter.input.messaging

import java.math.BigDecimal
import java.util.UUID

// Anti-corruption layer: mirrors FreelancerProfile's wire format without depending on its domain classes.
data class ProfileUpdatedIntegrationEvent(
    val freelancerId: UUID,
    val skills: Set<String>,
    val expectedDailyRateAmount: BigDecimal,
    val expectedDailyRateCurrency: String,
)
