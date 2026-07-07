package com.missionmatch.applicationtracking.application

import com.missionmatch.applicationtracking.application.port.input.GetCandidaturesUseCase
import com.missionmatch.applicationtracking.application.port.input.HandleMatchComputedUseCase
import com.missionmatch.applicationtracking.application.port.input.MatchComputedCommand
import com.missionmatch.applicationtracking.application.port.input.MoveCandidatureCommand
import com.missionmatch.applicationtracking.application.port.input.MoveCandidatureUseCase
import com.missionmatch.applicationtracking.application.port.output.CandidatureEventPublisher
import com.missionmatch.applicationtracking.application.port.output.CandidatureRepository
import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.event.CandidatureStatusChanged

class ApplicationTrackingService(
    private val candidatureRepository: CandidatureRepository,
    private val candidatureEventPublisher: CandidatureEventPublisher,
) : HandleMatchComputedUseCase, MoveCandidatureUseCase, GetCandidaturesUseCase {

    override fun handle(command: MatchComputedCommand) {
        // MatchComputed can be redelivered (Kafka is at-least-once): a reprocessed event must
        // not suggest the same candidature twice.
        if (candidatureRepository.existsByMissionIdAndFreelancerId(command.missionId, command.freelancerId)) {
            return
        }

        val candidature = Candidature.suggest(command.missionId, command.freelancerId)
        candidatureRepository.save(candidature)
        candidatureEventPublisher.publish(
            CandidatureStatusChanged(
                candidatureId = candidature.id,
                missionId = candidature.missionId,
                freelancerId = candidature.freelancerId,
                previousStatus = null,
                newStatus = candidature.status,
            ),
        )
    }

    override fun move(command: MoveCandidatureCommand) {
        val candidature = candidatureRepository.findById(command.candidatureId)
            ?: throw NoSuchElementException("No candidature found with id ${command.candidatureId}")

        val previousStatus = candidature.status
        candidature.moveTo(command.newStatus)
        candidatureRepository.save(candidature)
        candidatureEventPublisher.publish(
            CandidatureStatusChanged(
                candidatureId = candidature.id,
                missionId = candidature.missionId,
                freelancerId = candidature.freelancerId,
                previousStatus = previousStatus,
                newStatus = candidature.status,
            ),
        )
    }

    override fun getForFreelancer(freelancerId: FreelancerId): List<Candidature> =
        candidatureRepository.findByFreelancerId(freelancerId)
}
