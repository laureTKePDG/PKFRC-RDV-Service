# ── Stage 1 : Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Cache Maven dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q package -DskipTests --no-transfer-progress 2>/dev/null || \
    (apt-get install -y maven 2>/dev/null; mvn -q package -DskipTests --no-transfer-progress)

# Extract layers for optimized Docker caching
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ── Stage 2 : Runtime distroless ─────────────────────────────────────────────
FROM gcr.io/distroless/java21-debian12:nonroot

LABEL org.opencontainers.image.title="PKFRC RDV Service"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.description="Service de prise de RDV - PKFRC 2026"

WORKDIR /app

# Copie des layers Spring Boot dans l'ordre optimal de cache
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Utilisateur non-root (distroless:nonroot = uid 65532)
USER nonroot

EXPOSE 8080

# Tuning JVM pour conteneur K8s : ZGC low-latency + limites mémoire
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:MaxGCPauseMillis=50", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
  "org.springframework.boot.loader.launch.JarLauncher"]
