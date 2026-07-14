FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew fatJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/google-cloud-proxy-all.jar app.jar

ENV PORT=8003
EXPOSE 8003

ENTRYPOINT ["java", "-jar", "app.jar"]
