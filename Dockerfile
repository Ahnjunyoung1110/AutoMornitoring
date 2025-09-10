# --- build (gradle) ---
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
RUN ./gradlew --no-daemon build -x test || true
COPY . .
RUN ./gradlew --no-daemon clean bootJar

# --- runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# ffprobe/ffmpeg 설치
RUN apt-get update \
 && apt-get install -y --no-install-recommends ffmpeg \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app/app.jar"]
