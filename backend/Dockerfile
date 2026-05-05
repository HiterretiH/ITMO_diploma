# Use image Gradle (avoids downloading the wrapper distribution zip during docker build).
FROM gradle:9.4-jdk17-jammy AS builder
WORKDIR /home/gradle/project
COPY --chown=gradle:gradle . .
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
