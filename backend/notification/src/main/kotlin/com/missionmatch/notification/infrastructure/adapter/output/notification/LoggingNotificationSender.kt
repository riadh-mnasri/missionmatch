package com.missionmatch.notification.infrastructure.adapter.output.notification

import com.missionmatch.notification.application.port.output.NotificationSender
import com.missionmatch.notification.domain.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

// Stands in for a real email/Slack integration. Swapping to one is exactly what hexagonal
// architecture promises: implement NotificationSender differently, change nothing upstream of
// this adapter, since the application layer only ever depended on the port interface.
@Component
class LoggingNotificationSender : NotificationSender {

    override fun send(notification: Notification) {
        LOGGER.info("Notification for freelancer {}: {}", notification.recipientFreelancerId, notification.message)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LoggingNotificationSender::class.java)
    }
}
