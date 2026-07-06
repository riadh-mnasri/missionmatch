package com.missionmatch.sourcing.infrastructure.adapter.output.persistence

import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
import com.missionmatch.sourcing.domain.Mission
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MissionRepositoryAdapterTest {

    @Autowired
    private lateinit var jpaRepository: MissionJpaRepository

    private lateinit var repository: MissionRepositoryAdapter

    @BeforeEach
    fun setUp() {
        repository = MissionRepositoryAdapter(jpaRepository)
    }

    @Test
    fun `persists a mission and retrieves it back by id`() {
        // Given
        val mission = Mission.publish(
            title = "Kotlin backend developer",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin", "spring"),
            dailyRate = Money.of(600.0),
            startDate = LocalDate.now().plusWeeks(2),
        )

        // When
        repository.save(mission)
        val found = repository.findById(mission.id)

        // Then
        assertThat(found).isNotNull
        assertThat(found?.title).isEqualTo(mission.title)
        assertThat(found?.requiredSkills?.skills).isEqualTo(mission.requiredSkills.skills)
        assertThat(found?.dailyRate?.amount).isEqualByComparingTo(mission.dailyRate.amount)
    }

    @Test
    fun `returns null when no mission matches the given id`() {
        // Given
        val unknownMission = Mission.publish(
            title = "Never persisted",
            clientName = "Acme Corp",
            requiredSkills = SkillSet.of("kotlin"),
            dailyRate = Money.of(500.0),
            startDate = LocalDate.now(),
        )

        // When
        val found = repository.findById(unknownMission.id)

        // Then
        assertThat(found).isNull()
    }

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
