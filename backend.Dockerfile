# 1단계: Gradle을 사용하여 애플리케이션 빌드
FROM gradle:8.5.0-jdk17 AS builder

WORKDIR /workspace

# Gradle 래퍼 파일 복사
COPY gradlew .
COPY gradle ./gradle

# build.gradle 파일 복사
COPY build.gradle .
COPY settings.gradle .

# 소스 코드 복사
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
