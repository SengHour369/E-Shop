# Build context MUST be the repository root
FROM maven:3.9-amazoncorretto-17 AS builder

WORKDIR /build

# Copy the complete multi-module Maven reactor
COPY . .

# Build ai-service and required dependencies such as common-lib
RUN --mount=type=cache,id=eshop-maven,target=/root/.m2,sharing=locked \
    mvn -B -ntp -pl ai-service -am -DskipTests package


# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/ai-service/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
