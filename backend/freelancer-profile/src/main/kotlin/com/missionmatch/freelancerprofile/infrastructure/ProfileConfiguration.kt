package com.missionmatch.freelancerprofile.infrastructure

import com.missionmatch.freelancerprofile.application.ProfileApplicationService
import com.missionmatch.freelancerprofile.application.port.output.ProfileEventPublisher
import com.missionmatch.freelancerprofile.application.port.output.ProfileRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProfileConfiguration {

    @Bean
    fun profileApplicationService(
        profileRepository: ProfileRepository,
        profileEventPublisher: ProfileEventPublisher,
    ): ProfileApplicationService = ProfileApplicationService(profileRepository, profileEventPublisher)
}
