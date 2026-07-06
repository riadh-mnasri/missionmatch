package com.missionmatch.matching.infrastructure.adapter.input.web

import com.missionmatch.matching.application.port.input.GetMatchesUseCase
import com.missionmatch.matching.domain.FreelancerId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/matches")
class MatchController(
    private val getMatchesUseCase: GetMatchesUseCase,
) {

    @GetMapping
    fun getMatches(@RequestParam freelancerId: UUID): List<MatchResponse> =
        getMatchesUseCase.getMatchesForFreelancer(FreelancerId(freelancerId)).map { MatchResponse.from(it) }
}
