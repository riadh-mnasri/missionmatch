package com.missionmatch.freelancerprofile.domain.event

import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.shared.event.DomainEvent
import com.missionmatch.shared.event.EventMetadata
import java.math.BigDecimal

data class ProfileUpdated(
    val freelancerId: FreelancerId,
    val skills: Set<String>,
    val expectedDailyRateAmount: BigDecimal,
    val expectedDailyRateCurrency: String,
    override val metadata: EventMetadata = EventMetadata(),
) : DomainEvent
