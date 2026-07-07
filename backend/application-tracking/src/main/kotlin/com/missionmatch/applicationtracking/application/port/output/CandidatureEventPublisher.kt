package com.missionmatch.applicationtracking.application.port.output

import com.missionmatch.shared.event.DomainEvent

interface CandidatureEventPublisher {
    fun publish(event: DomainEvent)
}
