package com.missionmatch.freelancerprofile.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet

data class Profile(
    val freelancerId: FreelancerId,
    val skills: SkillSet,
    val expectedDailyRate: Money,
)
