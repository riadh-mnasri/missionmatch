package com.missionmatch.sourcing.infrastructure.adapter.input.web

import com.missionmatch.sourcing.application.port.input.CloseMissionUseCase
import com.missionmatch.sourcing.application.port.input.GetMissionUseCase
import com.missionmatch.sourcing.application.port.input.PublishMissionCommand
import com.missionmatch.sourcing.application.port.input.PublishMissionUseCase
import com.missionmatch.sourcing.domain.MissionId
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/missions")
class MissionController(
    private val publishMissionUseCase: PublishMissionUseCase,
    private val closeMissionUseCase: CloseMissionUseCase,
    private val getMissionUseCase: GetMissionUseCase,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun publish(@RequestBody request: PublishMissionRequest): MissionResponse {
        val command = PublishMissionCommand(
            title = request.title,
            clientName = request.clientName,
            requiredSkills = request.requiredSkills,
            dailyRateAmount = request.dailyRateAmount,
            startDate = request.startDate,
        )
        val missionId = publishMissionUseCase.publish(command)
        val mission = getMissionUseCase.getById(missionId)
            ?: error("Mission $missionId was just published but could not be reloaded")

        return MissionResponse.from(mission)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<MissionResponse> {
        val mission = getMissionUseCase.getById(MissionId(id)) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(MissionResponse.from(mission))
    }

    @PostMapping("/{id}/close")
    fun close(@PathVariable id: UUID): ResponseEntity<Void> {
        closeMissionUseCase.close(MissionId(id))
        return ResponseEntity.noContent().build()
    }
}
