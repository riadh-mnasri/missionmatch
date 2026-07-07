package com.missionmatch.freelancerprofile.infrastructure.adapter.output.persistence

import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "profiles")
class ProfileEntity(
    @Id
    val id: UUID,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "profile_skills", joinColumns = [JoinColumn(name = "freelancer_id")])
    @Column(name = "skill")
    val skills: Set<String>,

    @Column(nullable = false)
    val expectedDailyRateAmount: BigDecimal,

    @Column(nullable = false)
    val expectedDailyRateCurrency: String,
) {
    fun toDomain(): Profile = Profile(
        freelancerId = FreelancerId(id),
        skills = SkillSet(skills),
        expectedDailyRate = Money(expectedDailyRateAmount, expectedDailyRateCurrency),
    )

    companion object {
        fun fromDomain(profile: Profile): ProfileEntity = ProfileEntity(
            id = profile.freelancerId.value,
            skills = profile.skills.skills,
            expectedDailyRateAmount = profile.expectedDailyRate.amount,
            expectedDailyRateCurrency = profile.expectedDailyRate.currency,
        )
    }
}
