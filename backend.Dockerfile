# 실행을 위한 최소한의 JRE 이미지 사용
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 빌드 시 인자로 전달받은 JAR 파일을 복사
# GitHub Actions 호스트에서 이미 빌드된 파일을 사용함
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
