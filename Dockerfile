# ==============================================================
# Stage 1: Build — Maven + JDK 21
# ==============================================================
# Use official Eclipse Temurin JDK 21 on Maven 3.9 as the base image for building
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set the working directory inside the build container to /app
WORKDIR /app

# Copy the Maven project object model file (pom.xml) to the working directory first for cached layers
COPY pom.xml .

# Download project dependencies in quiet mode to cache them before copying source code
RUN mvn dependency:go-offline -q

# Copy the source code folder into the container's working directory
COPY src ./src

# Build and package the Java application as a JAR file, skipping tests, in quiet mode
RUN mvn package -DskipTests -q

# ==============================================================
# Stage 2: Runtime — minimal JRE 21
# ==============================================================
# Use minimal Alpine Linux base image containing only Java Runtime Environment (JRE) 21
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the runtime container to /app
WORKDIR /app

# Create a secure system user group and a non-root user to run the application securely
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR file from the builder stage into this runtime stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership of the copied JAR file to the non-root user and group
RUN chown appuser:appgroup app.jar

# Switch execution context to the secure non-root user
USER appuser

# Document that the container exposes REST port 5002
EXPOSE 5002

# Run the Spring Boot application using Java's jar execution tool
ENTRYPOINT ["java", "-jar", "app.jar"]
