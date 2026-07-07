package com.missionmatch.freelancerprofile.infrastructure.adapter.output.persistence

import com.missionmatch.freelancerprofile.domain.FreelancerId
import com.missionmatch.freelancerprofile.domain.Profile
import com.missionmatch.shared.domain.Money
import com.missionmatch.shared.domain.SkillSet
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
import java.util.UUID

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfileRepositoryAdapterTest {

    @Autowired
    private lateinit var jpaRepository: ProfileJpaRepository

    private lateinit var repository: ProfileRepositoryAdapter

    @BeforeEach
    fun setUp() {
        repository = ProfileRepositoryAdapter(jpaRepository)
    }

    @Test
    fun `persists a profile and retrieves it back by id`() {
        // Given
        val profile = Profile(
            freelancerId = FreelancerId(UUID.randomUUID()),
            skills = SkillSet.of("kotlin", "spring"),
            expectedDailyRate = Money.of(550.0),
        )

        // When
        repository.save(profile)
        val found = repository.findById(profile.freelancerId)

        // Then
        assertThat(found).isEqualTo(profile)
    }

    @Test
    fun `updating an existing profile overwrites its skills and rate`() {
        // Given
        val freelancerId = FreelancerId(UUID.randomUUID())
        repository.save(Profile(freelancerId, SkillSet.of("kotlin"), Money.of(500.0)))

        // When
        repository.save(Profile(freelancerId, SkillSet.of("kotlin", "kafka"), Money.of(600.0)))
        val found = repository.findById(freelancerId)

        // Then
        assertThat(found?.skills?.skills).containsExactlyInAnyOrder("kotlin", "kafka")
        assertThat(found?.expectedDailyRate?.amount).isEqualByComparingTo("600.0")
    }

    @Test
    fun `returns null when no profile matches the given id`() {
        // Given
        val unknownId = FreelancerId(UUID.randomUUID())

        // When
        val found = repository.findById(unknownId)

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
