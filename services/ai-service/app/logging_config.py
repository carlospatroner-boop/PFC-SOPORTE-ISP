"""Logging estructurado en JSON (Practica Experimental U5, item 2a: "logging
estructurado (JSON) en todos los microservicios"). Sin dependencia nueva
(nada de python-json-logger): un formatter propio sobre el modulo estandar
"logging" basta -- se aplica al logger raiz, asi que tambien cubre a los
loggers hijos como "ai-service.kafka_consumer" (ver kafka_consumer.py) sin
tocar ese archivo.
"""

import json
import logging
from datetime import datetime, timezone


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "@timestamp": datetime.fromtimestamp(record.created, tz=timezone.utc).isoformat(),
            "level": record.levelname.lower(),
            "service": "ai-service",
            "logger": record.name,
            "message": record.getMessage(),
        }
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)


def configure_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter())
    root = logging.getLogger()
    root.handlers = [handler]
    root.setLevel(logging.INFO)
