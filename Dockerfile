FROM maven:3.9-eclipse-temurin-8

WORKDIR /app

COPY pom.xml .
COPY api/ api/
COPY omod/ omod/

RUN mvn package -DskipTests
