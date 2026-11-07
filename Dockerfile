FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package


FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
