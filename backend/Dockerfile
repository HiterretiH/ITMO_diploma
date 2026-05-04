FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /src
COPY . .
RUN chmod +x gradlew \
  && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /src/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
