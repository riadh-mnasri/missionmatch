package com.missionmatch.matching.application.port.input

import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult

interface GetMatchesUseCase {
    fun getMatchesForFreelancer(freelancerId: FreelancerId): List<MatchResult>
}
