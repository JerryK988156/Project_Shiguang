# ============================================
# Stage 1: Build the Spring Boot application
# ============================================
FROM maven:3-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml mvnw ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ============================================
# Stage 2: Run the application
# ============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/shiguang-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
