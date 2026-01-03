# 1. Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
# Gradle 실행 권한 부여
RUN chmod +x ./gradlew
# 테스트를 제외하고 빌드 (초기 배포 점검용)
RUN ./gradlew bootJar -x test

# 2. Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 빌드된 Jar 파일을 실행 환경으로 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 운영 환경 프로파일 설정
ENV SPRING_PROFILES_ACTIVE=prod

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]

