package com.missionmatch.notification.application.port.output

import com.missionmatch.notification.domain.Notification

interface NotificationSender {
    fun send(notification: Notification)
}
