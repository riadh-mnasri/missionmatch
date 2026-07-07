package com.missionmatch.freelancerprofile.application

import com.missionmatch.freelancerprofile.application.port.input.GetProfileUseCase
import com.missionmatch.freelancerprofile.application.port.input.UpdateProfileCommand
import com.missionmatch.freelancerprofile.application.port.input.UpdateProfileUseCase
import com.missionmatch.freelancerprofile.application.port.output.ProfileEventPublisher
import com.missionmatch.freelancerprofile.application.port.output.ProfileRepository
import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile
import com.missionmatch.freelancerprofile.domain.event.ProfileUpdated
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet

class ProfileApplicationService(
    private val profileRepository: ProfileRepository,
    private val profileEventPublisher: ProfileEventPublisher,
) : UpdateProfileUseCase, GetProfileUseCase {

    override fun update(command: UpdateProfileCommand) {
        val profile = Profile(
            freelancerId = command.freelancerId,
            skills = SkillSet.of(command.skills),
            expectedDailyRate = Money(command.expectedDailyRateAmount, command.expectedDailyRateCurrency),
        )

        profileRepository.save(profile)
        profileEventPublisher.publish(
            ProfileUpdated(
                freelancerId = profile.freelancerId,
                skills = profile.skills.skills,
                expectedDailyRateAmount = profile.expectedDailyRate.amount,
                expectedDailyRateCurrency = profile.expectedDailyRate.currency,
            ),
        )
    }

    override fun getById(freelancerId: FreelancerId): Profile? = profileRepository.findById(freelancerId)
}
