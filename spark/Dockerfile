# Imagen para correr el pipeline de Spark igual en cualquier maquina del equipo
# (evita depender de winutils.exe/librerias nativas de Hadoop en Windows).
FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && apt-get install -y --no-install-recommends \
        python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

RUN python3 -m pip install --no-cache-dir \
        pandas numpy pyarrow "pyspark==4.1.2" scipy matplotlib

ENV PYTHONUNBUFFERED=1
WORKDIR /app/src

ENTRYPOINT ["python3"]
