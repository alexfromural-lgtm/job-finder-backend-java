# ==============================================================
# Stage 1: Build — Maven + JDK 21
# ==============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy dependency descriptor first for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# ==============================================================
# Stage 2: Runtime — minimal JRE 21
# ==============================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 5002

ENTRYPOINT ["java", "-jar", "app.jar"]
