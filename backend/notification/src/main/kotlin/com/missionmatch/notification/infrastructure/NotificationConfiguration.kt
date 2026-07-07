package com.missionmatch.notification.infrastructure

import com.missionmatch.notification.application.NotificationService
import com.missionmatch.notification.application.port.output.NotificationSender
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class NotificationConfiguration {

    @Bean
    fun notificationService(notificationSender: NotificationSender): NotificationService =
        NotificationService(notificationSender)
}
