"""Consumidor de "ticket.created" (Saga por coreografia con ticket-service):
clasifica cada ticket recien creado, guarda el resultado en Mongo, y publica
"ticket.classified" para que ticket-service complete category/priority/SLA.

Corre en un hilo de fondo (ver main.py) porque kafka-python es bloqueante/sincrono,
no async -- FastAPI sigue sirviendo /api/v1/ai/** normalmente en el hilo principal.
"""

import json
import logging
import threading
from datetime import datetime, timezone

from kafka import KafkaConsumer, KafkaProducer

from app.classifier import classify
from app.config import settings
from app.mongo import get_collection

logger = logging.getLogger("ai-service.kafka_consumer")

_producer: KafkaProducer | None = None


def _get_producer() -> KafkaProducer:
    global _producer
    if _producer is None:
        _producer = KafkaProducer(
            bootstrap_servers=settings.kafka_bootstrap_servers,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            key_serializer=lambda k: k.encode("utf-8") if k else None,
        )
    return _producer


def _handle_ticket_created(event: dict) -> None:
    ticket_id = event.get("ticketId")
    zone = event.get("zone")
    description = event.get("description") or ""

    result = classify(description)
    logger.info("Ticket %s clasificado como %s / %s", ticket_id, result.category, result.priority)

    get_collection().update_one(
        {"ticketId": ticket_id},
        {
            "$set": {
                "ticketId": ticket_id,
                "zone": zone,
                "description": description,
                "category": result.category,
                "priority": result.priority,
                "suggestion": result.suggestion,
                "classifiedAt": datetime.now(timezone.utc).isoformat(),
            }
        },
        upsert=True,
    )

    _get_producer().send(
        settings.topic_ticket_classified,
        key=ticket_id,
        value={"ticketId": ticket_id, "zone": zone, "category": result.category, "priority": result.priority},
    )
    _get_producer().flush()


def _consume_loop() -> None:
    logger.info("Conectando a Kafka en %s ...", settings.kafka_bootstrap_servers)
    # OJO: el parseo de JSON NO va en value_deserializer. kafka-python invoca ese
    # callback dentro de su maquinaria interna de fetch/poll, fuera del alcance del
    # try/except de este bucle -- un solo mensaje malformado ahi (por ejemplo, un
    # mensaje de prueba en texto plano publicado a mano) revienta el generador
    # "for message in consumer" por completo y mata el hilo consumidor para
    # siempre, en silencio (thread daemon). Por eso aqui se reciben bytes crudos y
    # el parseo ocurre dentro del try/except, mensaje por mensaje.
    consumer = KafkaConsumer(
        settings.topic_ticket_created,
        bootstrap_servers=settings.kafka_bootstrap_servers,
        group_id=settings.kafka_consumer_group_id,
        value_deserializer=lambda v: v,
        auto_offset_reset="earliest",
        enable_auto_commit=True,
    )
    logger.info("Escuchando el topico '%s'", settings.topic_ticket_created)
    for message in consumer:
        try:
            event = json.loads(message.value.decode("utf-8"))
            _handle_ticket_created(event)
        except Exception:
            # Un mensaje malformado o un fallo puntual de Mongo/Kafka no debe
            # tumbar el hilo consumidor -- se registra y se sigue con el siguiente.
            logger.exception("No se pudo procesar un mensaje de ticket.created: %s", message.value)


def start_consumer_thread() -> threading.Thread:
    thread = threading.Thread(target=_consume_loop, name="ticket-created-consumer", daemon=True)
    thread.start()
    return thread
