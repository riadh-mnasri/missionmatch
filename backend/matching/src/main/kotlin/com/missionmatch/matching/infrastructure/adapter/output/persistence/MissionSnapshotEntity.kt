package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.domain.MissionId
import com.missionmatch.matching.domain.MissionSnapshot
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "mission_snapshots")
class MissionSnapshotEntity(
    @Id
    val id: UUID,

    @ElementCollection
    @CollectionTable(name = "mission_snapshot_skills", joinColumns = [JoinColumn(name = "mission_id")])
    @Column(name = "skill")
    val requiredSkills: Set<String>,

    @Column(nullable = false)
    val dailyRateAmount: BigDecimal,

    @Column(nullable = false)
    val dailyRateCurrency: String,

    @Column(nullable = false)
    val open: Boolean,
) {
    fun toDomain(): MissionSnapshot = MissionSnapshot(
        missionId = MissionId(id),
        requiredSkills = SkillSet(requiredSkills),
        dailyRate = Money(dailyRateAmount, dailyRateCurrency),
        open = open,
    )

    companion object {
        fun fromDomain(snapshot: MissionSnapshot): MissionSnapshotEntity = MissionSnapshotEntity(
            id = snapshot.missionId.value,
            requiredSkills = snapshot.requiredSkills.skills,
            dailyRateAmount = snapshot.dailyRate.amount,
            dailyRateCurrency = snapshot.dailyRate.currency,
            open = snapshot.open,
        )
    }
}
