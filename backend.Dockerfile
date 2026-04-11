# 실행을 위한 최소한의 JRE 이미지 사용 (Java 21)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 빌드 시 인자로 전달받은 JAR 파일을 복사
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 환경 변수를 통해 가상 스레드 설정을 주입할 수 있도록 설정
ENTRYPOINT ["java", "-Dspring.threads.virtual.enabled=${SPRING_THREADS_VIRTUAL_ENABLED:-false}", "-jar", "app.jar"]
