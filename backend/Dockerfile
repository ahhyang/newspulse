FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/* \
	&& useradd --system --uid 1001 newspulse
COPY --from=build /app/target/*.jar app.jar
USER newspulse
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=8 \
	CMD curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
