# ---------- Stage 1: build the jar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy only what Maven needs first, so the dependency download is cached between builds
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Windows line endings would break the script on Linux, so remove them just in case
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw -B -q dependency:go-offline

COPY src src
# Tests are run before deploying (mvnw clean verify), not while building the image
RUN ./mvnw -B -q package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run the app as root
RUN useradd --system --create-home appuser
COPY --from=build /workspace/target/*.jar /app/app.jar
USER appuser

# "prod" profile = no SQL logging, no stack traces, HTTPS-aware (see application-prod.properties)
ENV SPRING_PROFILES_ACTIVE=prod
# Railway sets PORT at runtime; 8080 is only the default
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
