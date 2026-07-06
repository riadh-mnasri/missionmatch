package com.missionmatch.matching.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet

data class ProfileSnapshot(
    val freelancerId: FreelancerId,
    val skills: SkillSet,
    val expectedDailyRate: Money,
)
