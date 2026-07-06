package com.missionmatch.matching.infrastructure.adapter.input.messaging

import com.missionmatch.matching.application.port.input.HandleProfileUpdatedUseCase
import com.missionmatch.matching.application.port.input.ProfileUpdatedCommand
import com.missionmatch.matching.domain.FreelancerId
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProfileUpdatedConsumer(
    private val handleProfileUpdatedUseCase: HandleProfileUpdatedUseCase,
) {

    @KafkaListener(topics = [PROFILE_UPDATED_TOPIC], groupId = "matching")
    fun onProfileUpdated(event: ProfileUpdatedIntegrationEvent) {
        handleProfileUpdatedUseCase.handle(
            ProfileUpdatedCommand(
                freelancerId = FreelancerId(event.freelancerId),
                skills = event.skills,
                expectedDailyRateAmount = event.expectedDailyRateAmount,
                expectedDailyRateCurrency = event.expectedDailyRateCurrency,
            ),
        )
    }

    companion object {
        const val PROFILE_UPDATED_TOPIC = "profile-updated"
    }
}
