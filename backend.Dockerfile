# 1단계: Gradle을 사용하여 애플리케이션 빌드
FROM gradle:8.5.0-jdk17 AS builder

WORKDIR /workspace

# Gradle 래퍼 및 빌드 설정 파일만 먼저 복사
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# gradlew에 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성을 먼저 다운로드하여 별도의 레이어에 캐시
RUN ./gradlew dependencies

# 나머지 소스 코드 복사
COPY src ./src

# Gradle 빌드 실행 (테스트 제외)
RUN ./gradlew build -x test

# 2단계: 실제 실행을 위한 최소한의 이미지 생성
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일을 최종 이미지로 복사
COPY --from=builder /workspace/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
