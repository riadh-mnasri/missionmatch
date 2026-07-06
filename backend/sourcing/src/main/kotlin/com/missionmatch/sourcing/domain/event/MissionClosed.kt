package com.missionmatch.sourcing.domain.event

import com.missionmatch.shared.event.DomainEvent
import com.missionmatch.shared.event.EventMetadata
import com.missionmatch.sourcing.domain.MissionId

data class MissionClosed(
    val missionId: MissionId,
    override val metadata: EventMetadata = EventMetadata(),
) : DomainEvent
