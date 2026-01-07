# Compile
FROM gradle:9.2.1-jdk25 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

# Add wget for healthchecks
FROM busybox AS busybox

# Build minimal runtime image
FROM gcr.io/distroless/java25-debian13
COPY --from=build /app/build/libs/Server.jar /Server.jar
COPY --from=busybox /bin/wget /usr/bin/wget
EXPOSE 8030
CMD ["Server.jar"]
