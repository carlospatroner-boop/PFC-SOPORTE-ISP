"""ai-service — clasificacion asincrona de tickets via Kafka (equipo ACC).

Responsabilidad (ver tabla de microservicios de la Entrega 2): consumir
"ticket.created", clasificar categoria/prioridad + generar una sugerencia, y
publicar "ticket.classified" para que ticket-service complete el ticket.

Clasificador basado en reglas/palabras clave, NO un modelo de NLP entrenado --
ver app/classifier.py para la justificacion (no hay dataset etiquetado real).
"""

import logging

from fastapi import FastAPI, HTTPException

from app.config import settings
from app.kafka_consumer import start_consumer_thread
from app.mongo import get_collection

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("ai-service")

app = FastAPI(title="ai-service", description="Clasificacion de tickets via Kafka (equipo ACC)")


@app.on_event("startup")
def on_startup() -> None:
    start_consumer_thread()
    logger.info("ai-service arriba en el puerto %s", settings.port)


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.get("/api/v1/ai/classifications/{ticket_id}")
def get_classification(ticket_id: str) -> dict:
    doc = get_collection().find_one({"ticketId": ticket_id}, {"_id": 0})
    if doc is None:
        raise HTTPException(status_code=404, detail=f"No hay clasificacion todavia para el ticket {ticket_id}")
    return doc


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=settings.port)
