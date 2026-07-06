package com.missionmatch.matching.domain

class MatchingPolicy {

    fun score(mission: MissionSnapshot, profile: ProfileSnapshot): MatchingScore {
        val weighted = SKILL_WEIGHT * skillOverlapRatio(mission, profile) + RATE_WEIGHT * rateCompatibility(mission, profile)
        return MatchingScore(weighted.coerceIn(0.0, 1.0))
    }

    private fun skillOverlapRatio(mission: MissionSnapshot, profile: ProfileSnapshot): Double =
        mission.requiredSkills.overlapRatioWith(profile.skills)

    private fun rateCompatibility(mission: MissionSnapshot, profile: ProfileSnapshot): Double {
        if (mission.dailyRate.currency != profile.expectedDailyRate.currency) return 0.0

        val missionAmount = mission.dailyRate.amount.toDouble()
        val expectedAmount = profile.expectedDailyRate.amount.toDouble()
        if (expectedAmount <= 0.0) return 1.0

        return (missionAmount / expectedAmount).coerceIn(0.0, 1.0)
    }

    companion object {
        private const val SKILL_WEIGHT = 0.7
        private const val RATE_WEIGHT = 0.3
    }
}
