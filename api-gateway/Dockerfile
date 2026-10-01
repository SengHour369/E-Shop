# Build context is the repo root (multi-module Maven reactor)
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /build
COPY . .
RUN mvn -pl api-gateway -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
