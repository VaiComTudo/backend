# Stage 1: Build the Java application
FROM maven:3.9.5-amazoncorretto-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src /app/src

# Package the application (adjust if you use gradle)
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
