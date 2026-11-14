FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/kaichaini-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 10000
CMD ["sh", "-c", "exec java -jar app.jar --server.address=0.0.0.0 --server.port=${PORT:-10000}"]
