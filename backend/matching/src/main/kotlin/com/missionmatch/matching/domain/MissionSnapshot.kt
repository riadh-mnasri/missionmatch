package com.missionmatch.matching.domain

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet

data class MissionSnapshot(
    val missionId: MissionId,
    val requiredSkills: SkillSet,
    val dailyRate: Money,
    val open: Boolean,
)
