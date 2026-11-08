# Imagem única para todos os módulos Java: docker compose passa o nome do módulo em SERVICE.
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -q -B -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre
ARG SERVICE
RUN groupadd --system ava && useradd --system --gid ava --no-create-home ava
WORKDIR /app
COPY --from=build /build/${SERVICE}/target/app.jar app.jar
USER ava
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "app.jar"]
