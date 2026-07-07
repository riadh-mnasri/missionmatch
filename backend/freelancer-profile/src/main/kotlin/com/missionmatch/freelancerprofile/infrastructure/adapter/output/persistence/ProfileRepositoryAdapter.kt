package com.missionmatch.freelancerprofile.infrastructure.adapter.output.persistence

import com.missionmatch.freelancerprofile.application.port.output.ProfileRepository
import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile
import org.springframework.stereotype.Component

@Component
class ProfileRepositoryAdapter(
    private val jpaRepository: ProfileJpaRepository,
) : ProfileRepository {

    override fun save(profile: Profile): Profile =
        jpaRepository.save(ProfileEntity.fromDomain(profile)).toDomain()

    override fun findById(freelancerId: FreelancerId): Profile? =
        jpaRepository.findById(freelancerId.value).map { it.toDomain() }.orElse(null)
}
