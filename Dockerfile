FROM gradle:9.5.1-jdk21-alpine AS build

WORKDIR /app

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle ./
COPY build.gradle ./

RUN ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM amazoncorretto:21-alpine3.24-jdk

WORKDIR /app

COPY --from=build /app/build/libs/currency-service-0.0.1-SNAPSHOT.jar ./app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]