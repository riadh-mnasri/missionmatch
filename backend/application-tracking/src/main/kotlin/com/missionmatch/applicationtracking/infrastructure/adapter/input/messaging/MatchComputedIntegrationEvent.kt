package com.missionmatch.applicationtracking.infrastructure.adapter.input.messaging

import java.util.UUID

// Anti-corruption layer: mirrors Matching's wire format without depending on its domain classes.
data class MatchComputedIntegrationEvent(
    val missionId: UUID,
    val freelancerId: UUID,
    val score: Double,
)
