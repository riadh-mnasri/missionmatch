package com.missionmatch.sourcing.infrastructure.adapter.input.web

import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class PublishMissionRequest(
    val title: String,
    val clientName: String,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val startDate: LocalDate,
)

data class MissionResponse(
    val id: UUID,
    val title: String,
    val clientName: String,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val startDate: LocalDate,
    val status: MissionStatus,
) {
    companion object {
        fun from(mission: Mission): MissionResponse = MissionResponse(
            id = mission.id.value,
            title = mission.title,
            clientName = mission.clientName,
            requiredSkills = mission.requiredSkills.skills,
            dailyRateAmount = mission.dailyRate.amount,
            startDate = mission.startDate,
            status = mission.status,
        )
    }
}
