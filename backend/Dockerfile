FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 newspulse
COPY --from=build /app/target/*.jar app.jar
USER newspulse
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
