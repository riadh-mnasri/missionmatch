package com.missionmatch.applicationtracking.infrastructure.adapter.input.web

import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.CandidatureStatus
import java.util.UUID

data class UpdateCandidatureStatusRequest(
    val status: CandidatureStatus,
)

data class CandidatureResponse(
    val id: UUID,
    val missionId: UUID,
    val freelancerId: UUID,
    val status: CandidatureStatus,
) {
    companion object {
        fun from(candidature: Candidature): CandidatureResponse = CandidatureResponse(
            id = candidature.id.value,
            missionId = candidature.missionId.value,
            freelancerId = candidature.freelancerId.value,
            status = candidature.status,
        )
    }
}
