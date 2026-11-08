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
# 이미지에 curl/wget이 없어서 bash의 /dev/tcp로 /actuator/health를 직접 호출해 "UP"인지 확인한다.
# (/dev/tcp는 bash 전용이다. sh(dash)로 실행하면 항상 실패한다.)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
