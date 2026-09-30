# 빌드 단계. 래퍼로 받은 그래들을 쓴다
FROM amazoncorretto:21-alpine AS build
WORKDIR /build

# 의존성만 먼저 받아 두면 소스만 바뀐 빌드에서 이 층을 다시 쓴다
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM amazoncorretto:21-alpine
WORKDIR /app

# 루트로 돌리지 않는다
RUN addgroup -S kassa && adduser -S kassa -G kassa
USER kassa

COPY --from=build /build/build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너에 준 메모리에 맞춰 힙을 잡는다
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
