# Build and run Kotlin/Java server
FROM gradle:9.2.1-jdk25 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:25-jdk
COPY --from=build /app/build/libs/Server.jar /Server.jar
EXPOSE 8030
CMD ["java", "-jar", "/Server.jar"]
