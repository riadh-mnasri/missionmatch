plugins {
    id("org.springframework.boot")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":shared-kernel"))
    implementation(project(":sourcing"))
    implementation(project(":freelancer-profile"))
    implementation(project(":matching"))
    implementation(project(":application-tracking"))
    implementation(project(":notification"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Only pulled in for the "aws" profile, to authenticate to MSK Serverless with the ECS
    // task's IAM role instead of a broker-managed username/password.
    implementation("software.amazon.msk:aws-msk-iam-auth:2.2.0")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
