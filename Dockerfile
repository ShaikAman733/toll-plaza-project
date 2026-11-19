# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# copy only the build files first so dependency downloads are cached between builds
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/toll-plaza-finder-1.0.0.jar app.jar
USER app
# Render injects PORT; the app reads it (server.port=${PORT:8080})
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
