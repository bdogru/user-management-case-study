FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
# Tests run in CI and with ./mvnw verify; the image only packages the application.
RUN sh mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=build /app/target/user-management-0.0.1-SNAPSHOT.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
