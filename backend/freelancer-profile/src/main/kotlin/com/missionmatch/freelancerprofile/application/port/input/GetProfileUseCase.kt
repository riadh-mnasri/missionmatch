package com.missionmatch.freelancerprofile.application.port.input

import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile

interface GetProfileUseCase {
    fun getById(freelancerId: FreelancerId): Profile?
}
