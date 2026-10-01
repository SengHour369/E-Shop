# Build context is the repo root (multi-module Maven reactor)
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib ./common-lib
COPY discovery-server ./discovery-server
RUN mvn -pl discovery-server -am -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/discovery-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
