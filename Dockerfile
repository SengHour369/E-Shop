# Use official OpenJDK
FROM openjdk:27-ea-slim

# Set working directory
WORKDIR /app

# Copy your JAR
COPY target/Learning_spring_security-0.0.1-SNAPSHOT.jar app.jar

# Expose Spring Boot port (match your application.properties)
EXPOSE 8083

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]