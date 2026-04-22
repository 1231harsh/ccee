# Stage 1: Build (Java 21)
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy everything (since project is at root now)
COPY . .

# Build jar
RUN mvn clean package -DskipTests


# Stage 2: Run (lightweight)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]