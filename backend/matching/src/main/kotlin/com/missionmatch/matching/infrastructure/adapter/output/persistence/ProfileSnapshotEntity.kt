package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.ProfileSnapshot
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
@Table(name = "profile_snapshots")
class ProfileSnapshotEntity(
    @Id
    val id: UUID,

    @ElementCollection
    @CollectionTable(name = "profile_snapshot_skills", joinColumns = [JoinColumn(name = "freelancer_id")])
    @Column(name = "skill")
    val skills: Set<String>,

    @Column(nullable = false)
    val expectedDailyRateAmount: BigDecimal,

    @Column(nullable = false)
    val expectedDailyRateCurrency: String,
) {
    fun toDomain(): ProfileSnapshot = ProfileSnapshot(
        freelancerId = FreelancerId(id),
        skills = SkillSet(skills),
        expectedDailyRate = Money(expectedDailyRateAmount, expectedDailyRateCurrency),
    )

    companion object {
        fun fromDomain(snapshot: ProfileSnapshot): ProfileSnapshotEntity = ProfileSnapshotEntity(
            id = snapshot.freelancerId.value,
            skills = snapshot.skills.skills,
            expectedDailyRateAmount = snapshot.expectedDailyRate.amount,
            expectedDailyRateCurrency = snapshot.expectedDailyRate.currency,
        )
    }
}
