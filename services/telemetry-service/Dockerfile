# Etapa 1: Compilacion
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de produccion (JRE solamente)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Agente Java de OpenTelemetry (Modulo F) -- ver el comentario completo en el
# Dockerfile de svc-principal.
RUN wget -q -O /app/otel-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

USER appuser

COPY --from=builder /app/target/telemetry-service.jar app.jar

# 8006: HTTP (/actuator/health) -- 9500: servidor de sockets TCP (canal de telemetria,
# PE-U1) -- 9095: servidor gRPC (consulta del registro ya ordenado por Lamport).
EXPOSE 8006 9500 9095

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8006/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java -javaagent:/app/otel-agent.jar $JAVA_OPTS -jar app.jar"]
