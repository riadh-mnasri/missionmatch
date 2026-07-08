# Multi-stage build: compile with the full JDK + Gradle, ship only the JRE + jar.
# Build context is backend/, so this Dockerfile sees every module's sources.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY shared-kernel shared-kernel
COPY sourcing sourcing
COPY freelancer-profile freelancer-profile
COPY matching matching
COPY application-tracking application-tracking
COPY notification notification
COPY bootstrap bootstrap

RUN ./gradlew :bootstrap:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home missionmatch
USER missionmatch

COPY --from=build /workspace/bootstrap/build/libs/*.jar app.jar

EXPOSE 8181
ENTRYPOINT ["java", "-jar", "app.jar"]
