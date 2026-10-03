# Build context is the repo root (multi-module Maven reactor)
FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /build

# Copy the complete Maven multi-module project
COPY . .

# Build payment-service and its required modules
RUN --mount=type=cache,id=eshop-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl payment-service -am -DskipTests package


# Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/payment-service/target/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
