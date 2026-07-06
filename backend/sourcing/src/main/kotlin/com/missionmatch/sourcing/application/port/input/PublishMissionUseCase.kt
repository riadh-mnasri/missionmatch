package com.missionmatch.sourcing.application.port.input

import com.missionmatch.sourcing.domain.MissionId
import java.math.BigDecimal
import java.time.LocalDate

data class PublishMissionCommand(
    val title: String,
    val clientName: String,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val startDate: LocalDate,
)

interface PublishMissionUseCase {
    fun publish(command: PublishMissionCommand): MissionId
}
