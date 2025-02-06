#
# Build stage
#
FROM maven:3.8.7-openjdk-18-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM eclipse-temurin:18-jdk-alpine
COPY --from=build home/app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]