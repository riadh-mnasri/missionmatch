package com.missionmatch.sourcing.domain

import java.util.UUID

data class MissionId(val value: UUID) {
    companion object {
        fun generate(): MissionId = MissionId(UUID.randomUUID())
    }
}
