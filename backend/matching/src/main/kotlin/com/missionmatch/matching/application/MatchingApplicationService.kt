package com.missionmatch.matching.application

import com.missionmatch.matching.application.port.input.GetMatchesUseCase
import com.missionmatch.matching.application.port.input.HandleMissionPublishedUseCase
import com.missionmatch.matching.application.port.input.HandleProfileUpdatedUseCase
import com.missionmatch.matching.application.port.input.MissionPublishedCommand
import com.missionmatch.matching.application.port.input.ProfileUpdatedCommand
import com.missionmatch.matching.application.port.output.MatchEventPublisher
import com.missionmatch.matching.application.port.output.MatchResultRepository
import com.missionmatch.matching.application.port.output.MissionSnapshotRepository
import com.missionmatch.matching.application.port.output.ProfileSnapshotRepository
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.MatchResult
import com.missionmatch.matching.domain.MatchingPolicy
import com.missionmatch.matching.domain.MissionSnapshot
import com.missionmatch.matching.domain.ProfileSnapshot
import com.missionmatch.matching.domain.event.MatchComputed
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet

class MatchingApplicationService(
    private val missionSnapshotRepository: MissionSnapshotRepository,
    private val profileSnapshotRepository: ProfileSnapshotRepository,
    private val matchResultRepository: MatchResultRepository,
    private val matchEventPublisher: MatchEventPublisher,
    private val matchingPolicy: MatchingPolicy,
) : HandleMissionPublishedUseCase, HandleProfileUpdatedUseCase, GetMatchesUseCase {

    override fun handle(command: MissionPublishedCommand) {
        val mission = MissionSnapshot(
            missionId = command.missionId,
            requiredSkills = SkillSet.of(command.requiredSkills),
            dailyRate = Money(command.dailyRateAmount, command.dailyRateCurrency),
            open = true,
        )
        missionSnapshotRepository.save(mission)

        profileSnapshotRepository.findAll().forEach { profile -> computeAndPublish(mission, profile) }
    }

    override fun handle(command: ProfileUpdatedCommand) {
        val profile = ProfileSnapshot(
            freelancerId = command.freelancerId,
            skills = SkillSet.of(command.skills),
            expectedDailyRate = Money(command.expectedDailyRateAmount, command.expectedDailyRateCurrency),
        )
        profileSnapshotRepository.save(profile)

        missionSnapshotRepository.findAllOpen().forEach { mission -> computeAndPublish(mission, profile) }
    }

    override fun getMatchesForFreelancer(freelancerId: FreelancerId): List<MatchResult> =
        matchResultRepository.findByFreelancerId(freelancerId)

    private fun computeAndPublish(mission: MissionSnapshot, profile: ProfileSnapshot) {
        val score = matchingPolicy.score(mission, profile)
        if (!score.isAboveThreshold()) return

        val existing = matchResultRepository.findByMissionIdAndFreelancerId(mission.missionId, profile.freelancerId)
        // Kafka delivers at-least-once: the same event can be reprocessed (e.g. during a
        // consumer rebalance), so recomputing must update the existing match rather than
        // insert a duplicate row and re-fire a redundant MatchComputed event.
        if (existing != null && existing.score == score) return

        val matchResult = existing?.recompute(score) ?: MatchResult.compute(mission.missionId, profile.freelancerId, score)
        matchResultRepository.save(matchResult)
        matchEventPublisher.publish(
            MatchComputed(
                missionId = mission.missionId,
                freelancerId = profile.freelancerId,
                score = score.value,
            ),
        )
    }
}
