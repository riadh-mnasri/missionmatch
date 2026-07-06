package com.missionmatch.sourcing.infrastructure

import com.missionmatch.sourcing.application.MissionApplicationService
import com.missionmatch.sourcing.application.port.output.MissionEventPublisher
import com.missionmatch.sourcing.application.port.output.MissionRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SourcingConfiguration {

    @Bean
    fun missionApplicationService(
        missionRepository: MissionRepository,
        missionEventPublisher: MissionEventPublisher,
    ): MissionApplicationService = MissionApplicationService(missionRepository, missionEventPublisher)
}
