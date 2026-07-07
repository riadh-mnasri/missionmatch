package com.missionmatch.freelancerprofile.application.port.output

import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile

interface ProfileRepository {
    fun save(profile: Profile): Profile
    fun findById(freelancerId: FreelancerId): Profile?
}
