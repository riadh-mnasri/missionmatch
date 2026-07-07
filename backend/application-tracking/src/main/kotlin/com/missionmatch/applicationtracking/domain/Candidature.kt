package com.missionmatch.applicationtracking.domain

import com.fasterxml.jackson.annotation.JsonValue
import java.util.UUID

data class CandidatureId(@get:JsonValue val value: UUID) {
    companion object {
        fun generate(): CandidatureId = CandidatureId(UUID.randomUUID())
    }
}

class Candidature private constructor(
    val id: CandidatureId,
    val missionId: MissionId,
    val freelancerId: FreelancerId,
    status: CandidatureStatus,
) {
    var status: CandidatureStatus = status
        private set

    fun moveTo(newStatus: CandidatureStatus) {
        val allowed = ALLOWED_TRANSITIONS[status].orEmpty()
        require(newStatus in allowed) { "Cannot move a candidature from $status to $newStatus" }
        status = newStatus
    }

    companion object {
        fun suggest(missionId: MissionId, freelancerId: FreelancerId): Candidature =
            Candidature(CandidatureId.generate(), missionId, freelancerId, CandidatureStatus.TO_APPLY)

        fun reconstitute(
            id: CandidatureId,
            missionId: MissionId,
            freelancerId: FreelancerId,
            status: CandidatureStatus,
        ): Candidature = Candidature(id, missionId, freelancerId, status)

        private val ALLOWED_TRANSITIONS = mapOf(
            CandidatureStatus.TO_APPLY to setOf(CandidatureStatus.APPLIED),
            CandidatureStatus.APPLIED to setOf(CandidatureStatus.INTERVIEW, CandidatureStatus.REJECTED),
            CandidatureStatus.INTERVIEW to setOf(CandidatureStatus.ACCEPTED, CandidatureStatus.REJECTED),
        )
    }
}
