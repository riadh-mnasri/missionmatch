package com.missionmatch.matching.domain

data class MatchingScore(val value: Double) {

    init {
        require(value in 0.0..1.0) { "A matching score must be between 0.0 and 1.0" }
    }

    fun isAboveThreshold(): Boolean = value >= ELIGIBILITY_THRESHOLD

    companion object {
        const val ELIGIBILITY_THRESHOLD = 0.5
    }
}
