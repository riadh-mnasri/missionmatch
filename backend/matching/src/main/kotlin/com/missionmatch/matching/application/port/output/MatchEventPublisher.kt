package com.missionmatch.matching.application.port.output

import com.missionmatch.shared.event.DomainEvent

interface MatchEventPublisher {
    fun publish(event: DomainEvent)
}
