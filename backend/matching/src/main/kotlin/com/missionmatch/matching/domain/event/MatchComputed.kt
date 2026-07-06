package com.missionmatch.matching.domain.event

import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MissionId
import com.missionmatch.shared.event.DomainEvent
import com.missionmatch.shared.event.EventMetadata

data class MatchComputed(
    val missionId: MissionId,
    val freelancerId: FreelancerId,
    val score: Double,
    override val metadata: EventMetadata = EventMetadata(),
) : DomainEvent
