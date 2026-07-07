package com.missionmatch.notification.application

import com.missionmatch.notification.application.port.input.CandidatureStatusChangedNotificationCommand
import com.missionmatch.notification.application.port.input.MatchComputedNotificationCommand
import com.missionmatch.notification.application.port.output.NotificationSender
import com.missionmatch.notification.domain.Notification
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class NotificationServiceTest {

    private val notificationSender = mock<NotificationSender>()
    private lateinit var service: NotificationService

    @BeforeEach
    fun setUp() {
        service = NotificationService(notificationSender)
    }

    @Test
    fun `a computed match sends a notification to the matched freelancer`() {
        // Given
        val freelancerId = UUID.randomUUID()
        val command = MatchComputedNotificationCommand(
            missionId = UUID.randomUUID(),
            freelancerId = freelancerId,
            score = 0.85,
        )

        // When
        service.handle(command)

        // Then
        val captor = argumentCaptor<Notification>()
        verify(notificationSender).send(captor.capture())
        assertThat(captor.firstValue.recipientFreelancerId).isEqualTo(freelancerId)
        assertThat(captor.firstValue.message).contains("85%")
    }

    @Test
    fun `a candidature status change sends a notification describing the transition`() {
        // Given
        val freelancerId = UUID.randomUUID()
        val command = CandidatureStatusChangedNotificationCommand(
            candidatureId = UUID.randomUUID(),
            missionId = UUID.randomUUID(),
            freelancerId = freelancerId,
            previousStatus = "TO_APPLY",
            newStatus = "APPLIED",
        )

        // When
        service.handle(command)

        // Then
        val captor = argumentCaptor<Notification>()
        verify(notificationSender).send(captor.capture())
        assertThat(captor.firstValue.recipientFreelancerId).isEqualTo(freelancerId)
        assertThat(captor.firstValue.message).contains("TO_APPLY -> APPLIED")
    }
}
