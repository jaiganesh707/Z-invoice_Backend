FROM maven:3.9.6-eclipse-temurin-17 AS builder
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-jdk-alpine
COPY --from=builder target/z-invoice.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]