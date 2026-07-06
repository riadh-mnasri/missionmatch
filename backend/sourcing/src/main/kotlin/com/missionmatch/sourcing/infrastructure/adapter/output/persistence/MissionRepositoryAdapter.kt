package com.missionmatch.sourcing.infrastructure.adapter.output.persistence

import com.missionmatch.sourcing.application.port.output.MissionRepository
import com.missionmatch.sourcing.domain.Mission
import com.missionmatch.sourcing.domain.MissionId
import org.springframework.stereotype.Component

@Component
class MissionRepositoryAdapter(
    private val jpaRepository: MissionJpaRepository,
) : MissionRepository {

    override fun save(mission: Mission): Mission {
        val saved = jpaRepository.save(MissionEntity.fromDomain(mission))
        return saved.toDomain()
    }

    override fun findById(missionId: MissionId): Mission? =
        jpaRepository.findById(missionId.value).map { it.toDomain() }.orElse(null)

    override fun findAll(): List<Mission> = jpaRepository.findAll().map { it.toDomain() }
}
