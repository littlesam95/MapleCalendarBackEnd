# Multi-stage build를 사용하여 최적화된 이미지 생성
FROM gradle:8.14-jdk21 AS build
WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사 및 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# 실행 단계
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
# 외부 설정 파일 경로 지정 (볼륨 마운트된 resources 디렉토리 사용)
ENTRYPOINT exec java $JAVA_OPTS -Dspring.profiles.active=secret -Dspring.config.additional-location=file:/app/resources/ -jar app.jar