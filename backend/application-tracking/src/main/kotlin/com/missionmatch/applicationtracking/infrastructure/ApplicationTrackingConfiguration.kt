package com.missionmatch.applicationtracking.infrastructure

import com.missionmatch.applicationtracking.application.ApplicationTrackingService
import com.missionmatch.applicationtracking.application.port.output.CandidatureEventPublisher
import com.missionmatch.applicationtracking.application.port.output.CandidatureRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationTrackingConfiguration {

    @Bean
    fun applicationTrackingService(
        candidatureRepository: CandidatureRepository,
        candidatureEventPublisher: CandidatureEventPublisher,
    ): ApplicationTrackingService = ApplicationTrackingService(candidatureRepository, candidatureEventPublisher)
}
