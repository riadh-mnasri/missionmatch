package com.missionmatch.applicationtracking.infrastructure.adapter.input.messaging

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.support.serializer.JsonDeserializer

// match-computed has two independent consumers in this deployable (ApplicationTracking and
// Notification), each with its own anti-corruption DTO for the same topic. Two different
// consumer groups can use two entirely different deserializers for the same topic (Kafka
// doesn't care; deserialization is a per-consumer-group client concern), but the single global
// spring.json.type.mapping only maps an alias to one class - so this topic needs its own
// container factory instead of the shared default one.
//
// Named explicitly: Notification has its own same-named MatchComputedConsumerConfiguration
// (different package, so it compiles fine, but Spring's default bean name is the simple class
// name, and one shared context can't register two beans under it).
@Configuration("applicationTrackingMatchComputedConsumerConfiguration")
class MatchComputedConsumerConfiguration(
    @Value("\${spring.kafka.bootstrap-servers}") private val bootstrapServers: String,
) {

    @Bean
    fun applicationTrackingKafkaListenerContainerFactory():
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
