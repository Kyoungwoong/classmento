# syntax=docker/dockerfile:1
# classmento backend (Spring Boot 4.1 / Java 17)
# 빌드: docker build -t classmento-backend ./backend
# 실행: SPRING_PROFILES_ACTIVE=prod + DB_URL/DB_USERNAME/DB_PASSWORD + AI 키 환경변수 필요 (.env.example)

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
# 의존성 레이어 캐시: 빌드 스크립트가 안 바뀌면 재다운로드하지 않는다
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q bootJar -x test

FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --uid 10001 app \
 && mkdir -p /app/data/uploads && chown -R app:app /app
COPY --from=build --chown=app:app /src/build/libs/app.jar /app/app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    CLASSMENTO_STORAGE_ROOT=/app/data/uploads \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD ["sh", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 || exit 1"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
