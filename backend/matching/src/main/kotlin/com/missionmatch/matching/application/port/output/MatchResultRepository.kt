package com.missionmatch.matching.application.port.output

import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult

interface MatchResultRepository {
    fun save(matchResult: MatchResult): MatchResult
    fun findByFreelancerId(freelancerId: FreelancerId): List<MatchResult>
}
