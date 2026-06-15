FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY cardiology-cloud-common cardiology-cloud-common
COPY cardiology-session cardiology-session

RUN mvn -pl cardiology-session -am package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /build/cardiology-session/target/cardiology-session-*.jar app.jar

EXPOSE 30001

ENTRYPOINT ["java", "-jar", "app.jar"]
