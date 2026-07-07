package com.missionmatch.notification.domain

import java.util.UUID

// No aggregate here on purpose: this context protects no state worth its own consistency
// rules, so Notification is a plain value object, not an entity with an identity to track.
data class Notification(
    val recipientFreelancerId: UUID,
    val message: String,
)
