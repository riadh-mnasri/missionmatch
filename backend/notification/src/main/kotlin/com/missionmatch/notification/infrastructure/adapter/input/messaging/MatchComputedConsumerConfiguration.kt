package com.missionmatch.notification.infrastructure.adapter.input.messaging

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

// See ApplicationTracking's MatchComputedConsumerConfiguration for why this topic needs its own
// container factory: two contexts, two anti-corruption DTOs, one topic. Named explicitly for the
// same reason its sibling there is: a Spring bean registry name conflict, not a compile error.
@Configuration("notificationMatchComputedConsumerConfiguration")
class MatchComputedConsumerConfiguration(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    @Bean
    fun notificationKafkaListenerContainerFactory():
        ConcurrentKafkaListenerContainerFactory<String, MatchComputedIntegrationEvent> {
        val valueDeserializer = JsonDeserializer(MatchComputedIntegrationEvent::class.java, false).apply {
            addTrustedPackages("com.missionmatch.*")
        }
        val consumerFactory = DefaultKafkaConsumerFactory(
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ),
            StringDeserializer(),
            valueDeserializer,
        )
        return ConcurrentKafkaListenerContainerFactory<String, MatchComputedIntegrationEvent>().apply {
            this.consumerFactory = consumerFactory
        }
    }
}
