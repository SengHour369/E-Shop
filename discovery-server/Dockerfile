# Build context is the repo root (multi-module Maven reactor)
FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /build

# Copy the complete Maven multi-module project
COPY . .

# Build discovery-server and required dependencies
RUN --mount=type=cache,id=eshop-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl discovery-server -am -DskipTests package


# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/discovery-server/target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
