package com.missionmatch.applicationtracking.infrastructure.adapter.input.web

import com.missionmatch.applicationtracking.application.port.input.GetCandidaturesUseCase
import com.missionmatch.applicationtracking.application.port.input.MoveCandidatureCommand
import com.missionmatch.applicationtracking.application.port.input.MoveCandidatureUseCase
import com.missionmatch.applicationtracking.domain.CandidatureId
import com.missionmatch.applicationtracking.domain.FreelancerId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/candidatures")
class CandidatureController(
    private val getCandidaturesUseCase: GetCandidaturesUseCase,
    private val moveCandidatureUseCase: MoveCandidatureUseCase,
) {

    @GetMapping
    fun getForFreelancer(@RequestParam freelancerId: UUID): List<CandidatureResponse> =
        getCandidaturesUseCase.getForFreelancer(FreelancerId(freelancerId)).map { CandidatureResponse.from(it) }

    @PatchMapping("/{id}/status")
    fun updateStatus(@PathVariable id: UUID, @RequestBody request: UpdateCandidatureStatusRequest) {
        moveCandidatureUseCase.move(MoveCandidatureCommand(CandidatureId(id), request.status))
    }
}
