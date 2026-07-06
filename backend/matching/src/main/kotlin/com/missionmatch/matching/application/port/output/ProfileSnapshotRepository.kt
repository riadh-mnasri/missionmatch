package com.missionmatch.matching.application.port.output

import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.ProfileSnapshot

interface ProfileSnapshotRepository {
    fun save(snapshot: ProfileSnapshot): ProfileSnapshot
    fun findById(freelancerId: FreelancerId): ProfileSnapshot?
    fun findAll(): List<ProfileSnapshot>
}
