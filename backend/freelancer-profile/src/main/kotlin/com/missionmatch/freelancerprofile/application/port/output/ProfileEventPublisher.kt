package com.missionmatch.freelancerprofile.application.port.output

import com.missionmatch.shared.event.DomainEvent

interface ProfileEventPublisher {
    fun publish(event: DomainEvent)
}
