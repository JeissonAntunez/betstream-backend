# ============================================================
# BetStream API - Multi-stage Dockerfile
# Stage 1: Build with Maven
# Stage 2: Minimal JRE runtime image
# ============================================================

# ── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies first (layer cache optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -q 2>/dev/null || true

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

# Extract layered jar for better Docker caching
RUN java -Djarmode=layertools -jar target/betstream-api-*.jar extract

# ── Stage 2: Runtime ────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root
RUN addgroup -S betstream && adduser -S betstream -G betstream

WORKDIR /app

# Copy layered application (better cache hit rate on redeploy)
COPY --from=builder /build/dependencies/ ./
COPY --from=builder /build/spring-boot-loader/ ./
COPY --from=builder /build/snapshot-dependencies/ ./
COPY --from=builder /build/application/ ./

# Health check via actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

USER betstream

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "org.springframework.boot.loader.launch.JarLauncher"]
