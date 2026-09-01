# Etapa 1: Compilacion
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
# Reintenta hasta 3 veces con espera creciente (10s, 20s, 30s) si Maven Central /
# repo.spring.io no responde momentaneamente. Maven ya reintenta 3 veces a nivel
# HTTP por defecto (maven.wagon.http.retryHandler.count), pero eso no cubre una
# interrupcion de red mas larga como la observada en el run 33280543156:
# "Non-resolvable parent POM ... spring-boot-starter-parent:pom:3.2.5 (absent)".
# Este bucle reintenta el comando COMPLETO, no solo la peticion HTTP individual.
RUN for i in 1 2 3; do \
      mvn dependency:go-offline -B && exit 0; \
      echo "Intento $i de 'mvn dependency:go-offline' fallo, reintentando en $((i*10))s..."; \
      sleep $((i*10)); \
    done; \
    exit 1
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
COPY --from=builder /app/target/report-service.jar app.jar
EXPOSE 8005
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8005/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java -javaagent:/app/otel-agent.jar $JAVA_OPTS -jar app.jar"]
