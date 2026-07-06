package com.missionmatch.matching.infrastructure.adapter.output.persistence

import com.missionmatch.matching.application.port.output.MissionSnapshotRepository
import com.missionmatch.matching.domain.MissionId
import com.missionmatch.matching.domain.MissionSnapshot
import org.springframework.stereotype.Component

@Component
class MissionSnapshotRepositoryAdapter(
    private val jpaRepository: MissionSnapshotJpaRepository,
) : MissionSnapshotRepository {

    override fun save(snapshot: MissionSnapshot): MissionSnapshot =
        jpaRepository.save(MissionSnapshotEntity.fromDomain(snapshot)).toDomain()

    override fun findById(missionId: MissionId): MissionSnapshot? =
        jpaRepository.findById(missionId.value).map { it.toDomain() }.orElse(null)

    override fun findAllOpen(): List<MissionSnapshot> = jpaRepository.findByOpenTrue().map { it.toDomain() }
}
