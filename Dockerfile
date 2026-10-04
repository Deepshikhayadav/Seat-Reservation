# ---------------------------------------------------------
# Stage 1: Build the Spring Boot application
# ---------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

# Copy Maven wrapper and project files first.
# This allows Docker to cache dependencies.
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

RUN chmod +x mvnw

# Download dependencies.
RUN ./mvnw dependency:go-offline -B

# Copy source code.
COPY src src

# Build the application.
RUN ./mvnw clean package -DskipTests


# ---------------------------------------------------------
# Stage 2: Small production image
# ---------------------------------------------------------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Run as a non-root user.
RUN useradd --system --create-home appuser

COPY --from=build /app/target/*.jar app.jar

RUN chown appuser:appuser app.jar

USER appuser

# Render and most cloud platforms provide PORT.
# Spring Boot will use 8080 locally.
ENV PORT=8080

EXPOSE 8080

# Use the PORT environment variable if supplied.
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]