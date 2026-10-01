# Build context is the repo root (multi-module Maven reactor)
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib ./common-lib
COPY auth-service ./auth-service
RUN mvn -pl auth-service -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
