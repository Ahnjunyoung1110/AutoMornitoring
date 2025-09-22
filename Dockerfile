# --- build (Gradle) ---
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x ./gradlew
# 소스 복사
COPY . .
# 테스트 스킵하고 부트jar 생성 (실패 무시 금지)
RUN ./gradlew --no-daemon clean bootJar

# --- ffmpeg/ffprobe 제공 단계 ---
FROM jrottenberg/ffmpeg:8-ubuntu AS ffbin
# (확인용) RUN ffprobe -version && ffprobe -hide_banner -h demuxer=hls | head -n 20

# --- runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# ffmpeg/ffprobe + 필요한 라이브러리까지 복사
COPY --from=ffbin /usr/local/bin/ffprobe /usr/local/bin/ffprobe
COPY --from=ffbin /usr/local/bin/ffmpeg  /usr/local/bin/ffmpeg
COPY --from=ffbin /usr/local/lib/        /usr/local/lib/
ENV LD_LIBRARY_PATH=/usr/local/lib

# (선택) 루트 CA 보증서 최신화
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# 빌드 산출물 복사 (가능하면 bootJar 이름을 고정해두세요)
COPY --from=build /src/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-XX:+UseContainerSupport","-jar","/app/app.jar"]
