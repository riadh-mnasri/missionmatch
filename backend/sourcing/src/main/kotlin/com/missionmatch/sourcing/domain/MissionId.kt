package com.missionmatch.sourcing.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class MissionId(@get:JsonValue val value: UUID) {
    companion object {
        fun generate(): MissionId = MissionId(UUID.randomUUID())
    }
}
