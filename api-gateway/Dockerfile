# Build context must be the repository root
FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /build

# Copy the entire Maven multi-module project
COPY . .

# Build api-gateway and all required reactor dependencies
RUN --mount=type=cache,id=eshop-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl api-gateway -am -DskipTests package


# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
