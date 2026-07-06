package com.missionmatch.sourcing.infrastructure.adapter.output.messaging

import com.missionmatch.sourcing.domain.MissionId
import com.missionmatch.sourcing.domain.event.MissionPublished
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate
import java.util.Properties

@Testcontainers
class KafkaMissionEventPublisherTest {

    private lateinit var producerFactory: DefaultKafkaProducerFactory<String, Any>
    private lateinit var publisher: KafkaMissionEventPublisher

    @BeforeEach
    fun setUp() {
        val producerProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java,
        )
        producerFactory = DefaultKafkaProducerFactory(producerProps)
        publisher = KafkaMissionEventPublisher(KafkaTemplate(producerFactory))
    }

    @AfterEach
    fun tearDown() {
        producerFactory.destroy()
    }

    @Test
    fun `publishing MissionPublished sends a message on the mission-published topic`() {
        // Given
        val event = MissionPublished(
            missionId = MissionId.generate(),
            requiredSkills = setOf("kotlin", "spring"),
            dailyRateAmount = BigDecimal.valueOf(600),
            startDate = LocalDate.now().plusWeeks(2),
        )

        // When
        publisher.publish(event)

        // Then
        val consumerProps = Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.bootstrapServers)
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
            put(ConsumerConfig.GROUP_ID_CONFIG, "sourcing-test-group")
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }
        KafkaConsumer<String, String>(consumerProps).use { consumer ->
            consumer.subscribe(listOf(KafkaMissionEventPublisher.MISSION_PUBLISHED_TOPIC))
            val records = consumer.poll(Duration.ofSeconds(10))
            assertThat(records.count()).isEqualTo(1)
            assertThat(records.first().key()).isEqualTo(event.missionId.value.toString())
        }
    }

    companion object {
        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
    }
}
