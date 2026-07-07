package com.missionmatch

import com.missionmatch.matching.application.port.input.HandleProfileUpdatedUseCase
import com.missionmatch.matching.application.port.input.ProfileUpdatedCommand
import com.missionmatch.matching.domain.FreelancerId
import com.missionmatch.sourcing.application.port.input.CloseMissionUseCase
import com.missionmatch.sourcing.application.port.input.PublishMissionCommand
import com.missionmatch.sourcing.application.port.input.PublishMissionUseCase
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Component
@Profile("demo")
class DemoDataSeeder(
    private val publishMissionUseCase: PublishMissionUseCase,
    private val closeMissionUseCase: CloseMissionUseCase,
    private val handleProfileUpdatedUseCase: HandleProfileUpdatedUseCase,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val publishedIds = DEMO_MISSIONS.map { publishMissionUseCase.publish(it) }
        CLOSED_MISSION_INDICES.forEach { index -> closeMissionUseCase.close(publishedIds[index]) }

        // Give Matching's Kafka consumer time to build its mission read model before we
        // seed a profile, since matches are only computed against missions it already knows.
        Thread.sleep(2000)

        handleProfileUpdatedUseCase.handle(
            ProfileUpdatedCommand(
                freelancerId = FreelancerId(DEMO_FREELANCER_ID),
                skills = setOf("kotlin", "spring boot", "spring", "kafka", "java", "rest"),
                expectedDailyRateAmount = BigDecimal.valueOf(600),
                expectedDailyRateCurrency = "EUR",
            ),
        )

        LOGGER.info("Demo data seeded. Look up matches for freelancer id {}", DEMO_FREELANCER_ID)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DemoDataSeeder::class.java)

        val DEMO_FREELANCER_ID: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

        private val DEMO_MISSIONS = listOf(
            PublishMissionCommand(
                title = "Senior Kotlin Backend Engineer",
                clientName = "Northwind Logistics",
                requiredSkills = setOf("kotlin", "spring boot", "kafka", "postgresql"),
                dailyRateAmount = BigDecimal.valueOf(650),
                startDate = LocalDate.now().plusWeeks(3),
            ),
            PublishMissionCommand(
                title = "Angular Frontend Developer",
                clientName = "Solstice Retail",
                requiredSkills = setOf("angular", "typescript", "rxjs"),
                dailyRateAmount = BigDecimal.valueOf(520),
                startDate = LocalDate.now().plusWeeks(1),
            ),
            PublishMissionCommand(
                title = "DevOps Engineer, AWS & Terraform",
                clientName = "Carbon Fintech",
                requiredSkills = setOf("aws", "terraform", "docker", "kubernetes"),
                dailyRateAmount = BigDecimal.valueOf(700),
                startDate = LocalDate.now().plusWeeks(2),
            ),
            PublishMissionCommand(
                title = "Data Engineer, ETL Pipelines",
                clientName = "Meridian Health",
                requiredSkills = setOf("python", "airflow", "sql", "spark"),
                dailyRateAmount = BigDecimal.valueOf(600),
                startDate = LocalDate.now().plusWeeks(4),
            ),
            PublishMissionCommand(
                title = "React Native Mobile Developer",
                clientName = "Fernbridge Media",
                requiredSkills = setOf("react native", "typescript", "graphql"),
                dailyRateAmount = BigDecimal.valueOf(550),
                startDate = LocalDate.now().plusWeeks(6),
            ),
            PublishMissionCommand(
                title = "Java Integration Specialist",
                clientName = "Anchor Insurance",
                requiredSkills = setOf("java", "spring", "kafka", "rest"),
                dailyRateAmount = BigDecimal.valueOf(580),
                startDate = LocalDate.now().plusWeeks(2),
            ),
        )

        // Indices chosen to not overlap with the demo freelancer's skills: Matching does not
        // yet consume MissionClosed (see README roadmap), so a closed mission that did overlap
        // would still show up as a high-scoring match, which would be a confusing demo.
        private val CLOSED_MISSION_INDICES = setOf(1, 3)
    }
}
