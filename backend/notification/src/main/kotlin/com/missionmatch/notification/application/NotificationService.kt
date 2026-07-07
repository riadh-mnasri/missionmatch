package com.missionmatch.notification.application

import com.missionmatch.notification.application.port.input.CandidatureStatusChangedNotificationCommand
import com.missionmatch.notification.application.port.input.HandleCandidatureStatusChangedUseCase
import com.missionmatch.notification.application.port.input.HandleMatchComputedUseCase
import com.missionmatch.notification.application.port.input.MatchComputedNotificationCommand
import com.missionmatch.notification.application.port.output.NotificationSender
import com.missionmatch.notification.domain.Notification

class NotificationService(
    private val notificationSender: NotificationSender,
) : HandleMatchComputedUseCase, HandleCandidatureStatusChangedUseCase {

    override fun handle(command: MatchComputedNotificationCommand) {
        val percentage = (command.score * 100).toInt()
        notificationSender.send(
            Notification(
                recipientFreelancerId = command.freelancerId,
                message = "New match found for mission ${command.missionId} ($percentage% fit)",
            ),
        )
    }

    override fun handle(command: CandidatureStatusChangedNotificationCommand) {
        val transition = command.previousStatus?.let { "$it -> ${command.newStatus}" } ?: command.newStatus
        notificationSender.send(
            Notification(
                recipientFreelancerId = command.freelancerId,
                message = "Candidature for mission ${command.missionId} moved: $transition",
            ),
        )
    }
}
