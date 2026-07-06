package com.missionmatch.sourcing.application

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import com.missionmatch.sourcing.application.port.input.CloseMissionUseCase
import com.missionmatch.sourcing.application.port.input.GetMissionUseCase
import com.missionmatch.sourcing.application.port.input.PublishMissionCommand
import com.missionmatch.sourcing.application.port.input.PublishMissionUseCase
import com.missionmatch.sourcing.application.port.output.MissionEventPublisher
import com.missionmatch.sourcing.application.port.output.MissionRepository
import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId
import com.missionmatch.sourcing.domain.event.MissionClosed
import com.missionmatch.sourcing.domain.event.MissionPublished

class MissionApplicationService(
    private val missionRepository: MissionRepository,
    private val missionEventPublisher: MissionEventPublisher,
) : PublishMissionUseCase, CloseMissionUseCase, GetMissionUseCase {

    override fun publish(command: PublishMissionCommand): MissionId {
        val mission = Mission.publish(
            title = command.title,
            clientName = command.clientName,
            requiredSkills = SkillSet.of(command.requiredSkills),
            dailyRate = Money(command.dailyRateAmount),
            startDate = command.startDate,
        )

        missionRepository.save(mission)
        missionEventPublisher.publish(
            MissionPublished(
                missionId = mission.id,
                requiredSkills = mission.requiredSkills.skills,
                dailyRateAmount = mission.dailyRate.amount,
                dailyRateCurrency = mission.dailyRate.currency,
                startDate = mission.startDate,
            ),
        )

        return mission.id
    }

    override fun close(missionId: MissionId) {
        val mission = missionRepository.findById(missionId)
            ?: throw NoSuchElementException("No mission found with id $missionId")

        mission.close()
        missionRepository.save(mission)
        missionEventPublisher.publish(MissionClosed(missionId = mission.id))
    }

    override fun getById(missionId: MissionId): Mission? = missionRepository.findById(missionId)
}
