package com.missionmatch.notification.infrastructure.adapter.input.messaging

import java.util.UUID

// Anti-corruption layer: mirrors Matching's wire format without depending on its domain classes.
data class MatchComputedIntegrationEvent(
    val missionId: UUID,
    val freelancerId: UUID,
    val score: Double,
)

// Anti-corruption layer: mirrors ApplicationTracking's wire format without depending on its
// domain classes. Statuses travel as plain strings here (this context has no CandidatureStatus
// enum of its own, and doesn't need one just to relay a message).
data class CandidatureStatusChangedIntegrationEvent(
    val candidatureId: UUID,
    val missionId: UUID,
    val freelancerId: UUID,
    val previousStatus: String?,
    val newStatus: String,
)
