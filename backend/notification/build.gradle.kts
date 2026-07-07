plugins {
    kotlin("plugin.spring")
}

dependencies {
    api(project(":shared-kernel"))

    // No web starter, no JPA, no Postgres: this context has no aggregate, no state worth
    // persisting, and no REST API. It only consumes Kafka events and sends notifications.
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}
