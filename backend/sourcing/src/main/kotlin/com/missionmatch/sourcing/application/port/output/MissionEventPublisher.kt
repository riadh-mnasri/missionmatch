package com.missionmatch.sourcing.application.port.output

import com.missionmatch.shared.event.DomainEvent

interface MissionEventPublisher {
    fun publish(event: DomainEvent)
}
