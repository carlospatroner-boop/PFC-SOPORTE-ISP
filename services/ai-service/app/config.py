"""Configuracion de ai-service via variables de entorno (mismo patron que los
application.yml de los servicios Java: todo tiene un default razonable para
correr en localhost sin configurar nada, y se puede sobreescribir en Docker)."""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    port: int = 8004

    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_consumer_group_id: str = "ai-service"
    topic_ticket_created: str = "ticket.created"
    topic_ticket_classified: str = "ticket.classified"

    mongo_uri: str = "mongodb://localhost:27017"
    mongo_db: str = "ai_db"
    mongo_collection: str = "classifications"

    class Config:
        env_prefix = "AI_SERVICE_"


settings = Settings()
