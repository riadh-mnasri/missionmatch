package com.missionmatch.sourcing.infrastructure.adapter.output.persistence

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId
import com.missionmatch.sourcing.domain.MissionStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.FetchType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "missions")
class MissionEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false)
    val clientName: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mission_required_skills", joinColumns = [jakarta.persistence.JoinColumn(name = "mission_id")])
    @Column(name = "skill")
    val requiredSkills: Set<String>,

    @Column(nullable = false)
    val dailyRateAmount: BigDecimal,

    @Column(nullable = false)
    val dailyRateCurrency: String,

    @Column(nullable = false)
    val startDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: MissionStatus,
) {
    fun toDomain(): Mission = Mission.reconstitute(
        id = MissionId(id),
        title = title,
        clientName = clientName,
        requiredSkills = SkillSet(requiredSkills),
        dailyRate = Money(dailyRateAmount, dailyRateCurrency),
        startDate = startDate,
        status = status,
    )

    companion object {
        fun fromDomain(mission: Mission): MissionEntity = MissionEntity(
            id = mission.id.value,
            title = mission.title,
            clientName = mission.clientName,
            requiredSkills = mission.requiredSkills.skills,
            dailyRateAmount = mission.dailyRate.amount,
            dailyRateCurrency = mission.dailyRate.currency,
            startDate = mission.startDate,
            status = mission.status,
        )
    }
}
