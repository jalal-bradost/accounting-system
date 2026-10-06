# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY common ./common
COPY accounting-service ./accounting-service
COPY contacts-service ./contacts-service
COPY hr-service ./hr-service
COPY expense-service ./expense-service
COPY inventory-service ./inventory-service
COPY purchase-service ./purchase-service
COPY sales-service ./sales-service
COPY pos-service ./pos-service
COPY assistant-service ./assistant-service
COPY infrastructure ./infrastructure

RUN mvn -q -DskipTests package -pl accounting-service/accounting-container -am

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && groupadd --system app && useradd --system --gid app app \
  && mkdir -p /app/data \
  && chown -R app:app /app

COPY --from=build /workspace/accounting-service/accounting-container/target/accounting-container-*.jar /app/app.jar

USER app
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx1024m"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://127.0.0.1:8080/api/v1/auth/security-config >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
