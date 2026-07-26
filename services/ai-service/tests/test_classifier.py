"""Pruebas puras del clasificador por palabras clave -- sin Kafka ni Mongo,
igual que la convencion de pruebas del resto del proyecto (TicketServiceTest,
AuthServiceTest, etc.: unidades puras primero, integracion aparte)."""

from app.classifier import classify


def test_classifies_total_outage_as_conectividad_critico():
    result = classify("No tengo internet, es un corte total en toda la zona, es urgente")
    assert result.category == "CONECTIVIDAD"
    assert result.priority == "CRITICO"
    assert result.suggestion


def test_classifies_dns_keyword():
    result = classify("Mi navegador dice que el DNS no resuelve el dominio")
    assert result.category == "DNS"


def test_classifies_slowness_as_velocidad_medio():
    result = classify("El internet esta muy lento desde ayer, la descarga lenta me molesta")
    assert result.category == "VELOCIDAD"
    assert result.priority == "MEDIO"


def test_classifies_hardware_keyword():
    result = classify("El router tiene la luz roja parpadeando, creo que es hardware dañado")
    assert result.category == "HARDWARE"


def test_classifies_configuration_keyword():
    result = classify("No me acuerdo la clave de red wifi, necesito ayuda con la configuracion")
    assert result.category == "CONFIGURACION"


def test_classifies_intermittent_as_alto_priority():
    result = classify("El servicio se cae de forma intermitente, a veces vuelve solo")
    assert result.priority == "ALTO"


def test_defaults_to_conectividad_medio_when_no_keywords_match():
    result = classify("Tengo un problema raro con mi servicio")
    assert result.category == "CONECTIVIDAD"
    assert result.priority == "MEDIO"


def test_every_category_has_a_suggestion():
    from app.classifier import SUGGESTIONS, CATEGORY_RULES, DEFAULT_CATEGORY

    all_categories = {label for label, _ in CATEGORY_RULES} | {DEFAULT_CATEGORY}
    assert all_categories <= SUGGESTIONS.keys()
