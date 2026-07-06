package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.application.port.output.ProfileSnapshotRepository
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.matching.domain.ProfileSnapshot
import org.springframework.stereotype.Component

@Component
class ProfileSnapshotRepositoryAdapter(
    private val jpaRepository: ProfileSnapshotJpaRepository,
) : ProfileSnapshotRepository {

    override fun save(snapshot: ProfileSnapshot): ProfileSnapshot =
        jpaRepository.save(ProfileSnapshotEntity.fromDomain(snapshot)).toDomain()

    override fun findById(freelancerId: FreelancerId): ProfileSnapshot? =
        jpaRepository.findById(freelancerId.value).map { it.toDomain() }.orElse(null)

    override fun findAll(): List<ProfileSnapshot> = jpaRepository.findAll().map { it.toDomain() }
}
