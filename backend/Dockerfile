# ---- build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
# eclipse-temurin already ships a non-root user (uid 1000); reuse it
COPY --from=build /build/target/securetravels-crm-0.1.0.jar app.jar
USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]