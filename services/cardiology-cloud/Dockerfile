FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

ARG APP_MODULE=cardiology-session

COPY pom.xml .
COPY cardiology-cloud-common cardiology-cloud-common
COPY cardiology-gateway cardiology-gateway
COPY cardiology-auth cardiology-auth
COPY cardiology-session cardiology-session
COPY cardiology-record cardiology-record

RUN mvn -pl ${APP_MODULE} -am clean package -Dmaven.test.skip=true -q \
    && cp "/build/${APP_MODULE}/target/${APP_MODULE}-"*.jar /build/app.jar \
    && jar tf /build/app.jar | grep -q 'BOOT-INF/'

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ARG APP_MODULE=cardiology-session
ARG APP_PORT=30001

COPY --from=build /build/app.jar app.jar

EXPOSE ${APP_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
