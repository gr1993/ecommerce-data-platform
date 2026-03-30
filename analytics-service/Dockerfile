# ----------- STEP 1: Build Stage -----------
FROM gradle:8.4.0-jdk17-alpine AS build

WORKDIR /app

# Gradle 캐시 최적화
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성만 미리 다운로드
RUN gradle dependencies || true

# 소스 복사
COPY . .

# JAR 빌드
RUN gradle clean build -x test --no-daemon


# ----------- STEP 2: Run Stage -----------
FROM bellsoft/liberica-runtime-container:jdk-17-musl

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]