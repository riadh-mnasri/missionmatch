package com.missionmatch.sourcing.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import java.time.LocalDate

class Mission private constructor(
    val id: MissionId,
    val title: String,
    val clientName: String,
    val requiredSkills: SkillSet,
    val dailyRate: Money,
    val startDate: LocalDate,
    status: MissionStatus,
) {
    var status: MissionStatus = status
        private set

    fun isEligibleForMatching(): Boolean = status == MissionStatus.OPEN

    fun close() {
        check(status == MissionStatus.OPEN) { "Only an open mission can be closed" }
        status = MissionStatus.CLOSED
    }

    companion object {
        fun publish(
            title: String,
            clientName: String,
            requiredSkills: SkillSet,
            dailyRate: Money,
            startDate: LocalDate,
        ): Mission {
            require(title.isNotBlank()) { "Mission title must not be blank" }
            require(clientName.isNotBlank()) { "Client name must not be blank" }
            return Mission(
                id = MissionId.generate(),
                title = title,
                clientName = clientName,
                requiredSkills = requiredSkills,
                dailyRate = dailyRate,
                startDate = startDate,
                status = MissionStatus.OPEN,
            )
        }

        fun reconstitute(
            id: MissionId,
            title: String,
            clientName: String,
            requiredSkills: SkillSet,
            dailyRate: Money,
            startDate: LocalDate,
            status: MissionStatus,
        ): Mission = Mission(id, title, clientName, requiredSkills, dailyRate, startDate, status)
    }
}
