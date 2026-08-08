# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn -q package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Pre-create the upload directory and give it to appuser. /app itself stays
# root-owned (the jar is not writable by the app); only ./uploads is writable.
# A named volume mounted here inherits this ownership, so the non-root process
# can persist files without world-writable permissions or running as root.
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app/uploads
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
