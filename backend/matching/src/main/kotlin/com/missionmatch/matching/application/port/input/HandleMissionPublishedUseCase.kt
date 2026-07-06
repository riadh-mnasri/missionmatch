package com.missionmatch.matching.application.port.input

import com.missionmatch.matching.domain.MissionId
import java.math.BigDecimal

data class MissionPublishedCommand(
    val missionId: MissionId,
    val requiredSkills: Set<String>,
    val dailyRateAmount: BigDecimal,
    val dailyRateCurrency: String,
)

interface HandleMissionPublishedUseCase {
    fun handle(command: MissionPublishedCommand)
}
