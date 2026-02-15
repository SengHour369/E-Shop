# ---------- Build Stage ----------
FROM  maven:4.0.0-rc-5-amazoncorretto AS builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ---------- Run Stage ----------
FROM openjdk:27-ea-slim

# Set working directory
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar
# Expose Spring Boot port
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
