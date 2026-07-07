package com.missionmatch.applicationtracking.application.port.input

import com.missionmatch.applicationtracking.domain.Candidature
import com.missionmatch.applicationtracking.domain.FreelancerId

interface GetCandidaturesUseCase {
    fun getForFreelancer(freelancerId: FreelancerId): List<Candidature>
}
