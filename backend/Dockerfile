# Stage 1: Build stage with pre-installed Gradle and JDK 21
FROM gradle:8.8-jdk21-alpine AS builder

WORKDIR /app

# Copy gradle configurations and source code
COPY build.gradle settings.gradle ./
COPY src ./src

# Build executable Spring Boot jar
RUN gradle bootJar --no-daemon

# Stage 2: Minimal runtime image with slim JRE 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root system user
RUN addgroup -S crowdshield && adduser -S crowdshield -G crowdshield
USER crowdshield:crowdshield

# Copy demo dataset for standalone operation
COPY --chown=crowdshield:crowdshield demo-data /demo-data

# Copy compiled jar from builder
COPY --from=builder --chown=crowdshield:crowdshield /app/build/libs/*.jar app.jar

# Expose backend API port
EXPOSE 8080

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
