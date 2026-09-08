# Informe JaCoCo real — svc-principal (ticket-service)

Generado corriendo `mvn test -Pcoverage` sobre el commit `31f1ef5` (más las correcciones de
esta misma revisión), en la máquina de desarrollo del equipo (host, sin anidar Docker — por
eso `TicketRepositoryIntegrationTest`, con Testcontainers, sí corrió aquí; ver limitación
conocida en `docs/experimentos/evaluacion_iso25010.md` sección 7 para el caso de Docker
anidado). 87 pruebas ejecutadas, todas verdes.

Abrir `index.html` en este directorio para el reporte navegable completo (por paquete y por
clase). `../jacoco-svc-principal-raw.exec` es el archivo binario crudo de cobertura
(`jacoco.exec`), para quien quiera regenerar el HTML o verificar los números con otra
herramienta.

## Resumen (total del proyecto)

| Métrica | Cobertura |
|---|---|
| Instrucciones | 72 % (2 582 de 3 582 cubiertas) |
| Ramas | 57 % (88 de 154 cubiertas) |
| Líneas | 81.1 % (551 de 679 cubiertas) |
| Métodos | 76 % (235 de 309 cubiertos) |
| Clases | 87 % (60 de 69 cubiertas) |

La cifra de **81.1 % en líneas** es la misma que ya se citaba en la Sección~7 de este mismo
directorio de experimentos (`docs/experimentos/evaluacion_iso25010.md`) y en la Sección de
Arquitectura del manuscrito — antes citada solo desde una tabla Markdown escrita a mano; este
reporte es la evidencia real que la respalda.

Excluye `ec/edu/uteq/soporte/telemetryservice/infrastructure/grpc/**` (código generado por
protobuf/gRPC a partir de `telemetry.proto`), configurado en `services/svc-principal/pom.xml`,
perfil `coverage`.
