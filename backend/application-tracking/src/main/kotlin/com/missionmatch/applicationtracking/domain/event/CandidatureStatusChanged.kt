package com.missionmatch.applicationtracking.domain.event

import com.missionmatch.applicationtracking.domain.CandidatureId
import com.missionmatch.applicationtracking.domain.CandidatureStatus
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId
import com.missionmatch.shared.event.DomainEvent
import com.missionmatch.shared.event.EventMetadata

data class CandidatureStatusChanged(
    val candidatureId: CandidatureId,
    val missionId: MissionId,
    val freelancerId: FreelancerId,
    val previousStatus: CandidatureStatus?,
    val newStatus: CandidatureStatus,
    override val metadata: EventMetadata = EventMetadata(),
) : DomainEvent
