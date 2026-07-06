package com.missionmatch.matching.infrastructure.adapter.input.messaging

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

// Anti-corruption layer: mirrors Sourcing's wire format without depending on its domain classes.
data class MissionPublishedIntegrationEvent(
    val missionId: UUID,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val dailyRateCurrency: String,
    val startDate: LocalDate,
)
