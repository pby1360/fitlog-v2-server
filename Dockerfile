FROM ubuntu:latest
LABEL authors="pby13"

ENTRYPOINT ["top", "-b"]

# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 그래들 빌드에 필요한 파일들 복사
COPY gradlew .
RUN chmod +x gradlew
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 종속성 먼저 다운로드 (캐싱 활용)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드 (테스트 제외로 속도 향상)
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 컨테이너 실행 시 환경 변수 설정
# Render에서 PORT 환경변수를 자동으로 주므로 이를 활용
ENV PORT=8080
EXPOSE ${PORT}

# 애플리케이션 실행 (운영 프로필 적용)
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-Dserver.port=${PORT}", "-jar", "app.jar"]