package com.missionmatch.matching.infrastructure

import com.missionmatch.matching.application.MatchingApplicationService
import com.missionmatch.matching.application.port.output.MatchEventPublisher
import com.missionmatch.matching.application.port.output.MatchResultRepository
import com.missionmatch.matching.application.port.output.MissionSnapshotRepository
import com.missionmatch.matching.application.port.output.ProfileSnapshotRepository
import com.missionmatch.matching.domain.MatchingPolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MatchingConfiguration {

    @Bean
    fun matchingPolicy(): MatchingPolicy = MatchingPolicy()

    @Bean
    fun matchingApplicationService(
        missionSnapshotRepository: MissionSnapshotRepository,
        profileSnapshotRepository: ProfileSnapshotRepository,
        matchResultRepository: MatchResultRepository,
        matchEventPublisher: MatchEventPublisher,
        matchingPolicy: MatchingPolicy,
    ): MatchingApplicationService = MatchingApplicationService(
        missionSnapshotRepository,
        profileSnapshotRepository,
        matchResultRepository,
        matchEventPublisher,
        matchingPolicy,
    )
}
