package com.missionmatch.applicationtracking.application.port.output

import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.CandidatureId
import com.missionmatch.applicationtracking.domain.FreelancerId
import com.missionmatch.applicationtracking.domain.MissionId

interface CandidatureRepository {
    fun save(candidature: Candidature): Candidature
    fun findById(candidatureId: CandidatureId): Candidature?
    fun findByFreelancerId(freelancerId: FreelancerId): List<Candidature>
    fun existsByMissionIdAndFreelancerId(missionId: MissionId, freelancerId: FreelancerId): Boolean
}
