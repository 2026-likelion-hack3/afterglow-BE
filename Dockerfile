# syntax=docker/dockerfile:1

# ---- build stage ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src

# 테스트는 실제 PostgreSQL에 의존(@SpringBootTest)해 이 단계에서 DB에 접근할 수 없다.
# 테스트 검증은 CI의 별도 Gradle 단계(Postgres service container)가 전담하고,
# 이 stage는 컴파일과 실행 가능한 jar 패키징만 한다.
RUN ./gradlew clean bootJar -x test --no-daemon

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown spring:spring app.jar

USER spring

EXPOSE 8080

# Spring profile, DB 접속 정보, JWT/AWS 설정 등은 이미지에 넣지 않고
# 컨테이너 실행 시 환경변수로 주입한다 (docs/deployment.md 참고).
ENTRYPOINT ["java", "-jar", "app.jar"]
