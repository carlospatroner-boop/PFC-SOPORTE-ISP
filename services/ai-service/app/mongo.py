"""Cliente de MongoDB para ai_service. Una coleccion simple: "classifications",
un documento por ticket clasificado (ver kafka_consumer.py)."""

from pymongo import MongoClient
from pymongo.collection import Collection

from app.config import settings

_client: MongoClient | None = None


def get_collection() -> Collection:
    global _client
    if _client is None:
        _client = MongoClient(settings.mongo_uri, serverSelectionTimeoutMS=5000)
    return _client[settings.mongo_db][settings.mongo_collection]
