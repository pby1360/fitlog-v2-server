# 1단계: 빌드 스테이지 (JDK 21로 변경)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# 그래들 빌드에 필요한 파일들 복사
COPY gradlew .
# 실행 권한 부여
RUN chmod +x gradlew
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 종속성 먼저 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon

# 2단계: 실행 스테이지 (JRE 21로 변경)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

ENV PORT=8080
EXPOSE ${PORT}

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-Dserver.port=${PORT}", "-jar", "app.jar"]