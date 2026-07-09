# Multi-stage build for Spring Boot application
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace/app

# Copy Maven wrapper and project metadata first to maximize layer caching.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies before copying source.
RUN ./mvnw -B dependency:go-offline

# Copy source and build jar.
COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install curl for healthcheck, then remove apt cache to keep layer small.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as non-root user.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app/target/*.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS="" \
	SPRING_PROFILES_ACTIVE=default

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
