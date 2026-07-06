package com.missionmatch.shared.event

import java.time.Instant
import java.util.UUID

data class EventMetadata(
    val eventId: UUID = UUID.randomUUID(),
    val occurredAt: Instant = Instant.now(),
    val correlationId: UUID = UUID.randomUUID(),
)
