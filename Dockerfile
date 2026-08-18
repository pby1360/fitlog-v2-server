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

# 비루트 실행 (Cloud Run 권장)
RUN addgroup -S app && adduser -S app -G app && chown app:app /app/app.jar
USER app

# 애플리케이션 실행
# -XX:MaxRAMPercentage : 컨테이너 메모리 대비 힙 상한(512Mi 기준 ~358m). 고정 -Xmx 대신 사용해 메모리 변경 시 자동 추종
# -XX:+UseSerialGC     : 1 vCPU 소규모 힙에서 G1보다 오버헤드·기동시간 유리
# -XX:TieredStopAtLevel=1 : C1까지만 JIT → 콜드스타트 단축 (min-instances=0 이라 기동속도 우선)
# -XX:+ExitOnOutOfMemoryError : OOM 시 즉시 종료 → Cloud Run이 인스턴스 교체 (반죽음 상태 방지)
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=70.0", \
  "-XX:+UseSerialGC", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]