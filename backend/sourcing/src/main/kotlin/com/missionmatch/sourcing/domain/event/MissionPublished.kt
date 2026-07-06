package com.missionmatch.sourcing.domain.event

import com.missionmatch.shared.event.DomainEvent
import com.missionmatch.shared.event.EventMetadata
import com.missionmatch.sourcing.domain.MissionId
import java.math.BigDecimal
import java.time.LocalDate

data class MissionPublished(
    val missionId: MissionId,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val dailyRateCurrency: String,
    val startDate: LocalDate,
    override val metadata: EventMetadata = EventMetadata(),
) : DomainEvent
