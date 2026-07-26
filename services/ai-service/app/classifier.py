"""Clasificador de tickets por palabras clave.

IMPORTANTE (honestidad de alcance -- mismo criterio que el dataset sintetico de
Spark en generate_dataset.py): no existe un dataset de tickets reales etiquetado
para entrenar un modelo de NLP de verdad, asi que esto es un clasificador basado
en reglas/palabras clave sobre el texto de la descripcion, NO un modelo de
Machine Learning entrenado. Se documenta asi explicitamente en el README y aqui
mismo -- es un placeholder razonable y facil de reemplazar mas adelante por un
modelo real si el equipo consigue datos etiquetados.

Las categorias/prioridades deben coincidir exactamente con los enums Category y
Priority de ticket-service (services/svc-principal/.../domain/).
"""

from dataclasses import dataclass

CATEGORY_RULES: list[tuple[str, list[str]]] = [
    ("DNS", ["dns", "no resuelve", "resolver dominio", "resolucion de nombres"]),
    ("VELOCIDAD", ["lento", "lentitud", "va lento", "demora", "velocidad baja", "descarga lenta"]),
    ("HARDWARE", ["router", "modem", "módem", "cable", "luz roja", "parpade", "hardware", "equipo dañado", "antena"]),
    ("CONFIGURACION", ["configuracion", "configuración", "wifi", "contraseña", "clave de red", "config"]),
    ("CONECTIVIDAD", ["no internet", "sin internet", "sin acceso", "sin servicio", "corte total", "no hay internet", "caído", "caido"]),
]

PRIORITY_RULES: list[tuple[str, list[str]]] = [
    ("CRITICO", ["toda la zona", "sin servicio total", "urgente", "empresa", "negocio", "corte total", "todos los equipos"]),
    ("ALTO", ["intermitente", "se cae", "a veces", "constantemente", "cada rato"]),
    ("MEDIO", ["lento", "lentitud", "demora", "molest"]),
]

SUGGESTIONS: dict[str, str] = {
    "DNS": "Verificar la configuracion de DNS del dispositivo o probar con un DNS publico (8.8.8.8 / 1.1.1.1).",
    "VELOCIDAD": "Ejecutar una prueba de velocidad y comparar contra el plan contratado; revisar saturacion en horas pico.",
    "HARDWARE": "Inspeccionar visualmente el equipo (luces, cables) y agendar visita tecnica si el hardware parece danado.",
    "CONFIGURACION": "Revisar la configuracion de red/WiFi del router; puede requerir un restablecimiento de fabrica.",
    "CONECTIVIDAD": "Reiniciar el router y verificar los cables de conexion; revisar si hay un corte reportado en la zona.",
}

DEFAULT_CATEGORY = "CONECTIVIDAD"
DEFAULT_PRIORITY = "MEDIO"


@dataclass(frozen=True)
class Classification:
    category: str
    priority: str
    suggestion: str


def _match(text: str, rules: list[tuple[str, list[str]]], default: str) -> str:
    lowered = text.lower()
    for label, keywords in rules:
        if any(keyword in lowered for keyword in keywords):
            return label
    return default


def classify(description: str) -> Classification:
    """Clasifica una descripcion de ticket en categoria + prioridad, y agrega una
    sugerencia canned por categoria. Funcion pura, sin dependencias externas --
    facil de probar sin Kafka ni Mongo (ver tests/test_classifier.py)."""
    category = _match(description, CATEGORY_RULES, DEFAULT_CATEGORY)
    priority = _match(description, PRIORITY_RULES, DEFAULT_PRIORITY)
    suggestion = SUGGESTIONS[category]
    return Classification(category=category, priority=priority, suggestion=suggestion)
