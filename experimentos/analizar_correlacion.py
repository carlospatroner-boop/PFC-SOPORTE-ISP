"""Primera medicion real de precision/exhaustividad del agrupamiento (Adicion 3 de la
Ampliacion del Modulo G, equipo ACC -- ver docs/adr/0008-correl-incidencias.md).

Compara el agrupamiento real que produjo la estrategia CORREL activa (consultado via
GET /api/v1/tickets/incidencias) contra la verdad de campo exacta que escribio
experimentos/inyector_averias.py en experimentos/resultados/verdad_campo.csv.

Esto es una corrida MANUAL para probar que el mecanismo completo funciona de punta a punta con
datos reales -- no es todavia la bateria estadistica completa de 100 corridas (10 repeticiones x
4 escenarios x 3 modos) que exige el protocolo del Modulo G ampliado; esa es una fase de trabajo
aparte (ver docs/adr/0008-correl-incidencias.md, seccion de consecuencias).

Definiciones (identicas a las de la guia de reutilizacion, seccion 5.2.3):
    Incidencias efectivas = cuantas incidencias abiertas debe atender el despachador
    Precision  = de los tickets que quedaron en la incidencia de la averia, que proporcion
                 realmente pertenecia a ella segun verdad_campo.csv
    Exhaustividad = de los tickets que si pertenecian a la averia, que proporcion termino
                    agrupada en esa incidencia

Uso (con el stack levantado, y ya corrido el inyector para esa zona):
    python experimentos/analizar_correlacion.py --zone QUEVEDO_CENTRO
"""
import argparse
import csv
import json
import urllib.request

GATEWAY = "http://localhost:8000"
CLIENTE_EMAIL = "cliente@test.com"
CLIENTE_PASSWORD = "Passw0rd!"
VERDAD_CAMPO_CSV = "experimentos/resultados/verdad_campo.csv"


def login():
    body = json.dumps({"email": CLIENTE_EMAIL, "password": CLIENTE_PASSWORD}).encode("utf-8")
    req = urllib.request.Request(
        f"{GATEWAY}/api/v1/auth/login", data=body,
        headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.load(resp)["data"]["accessToken"]


def obtener_incidencias(token, zone):
    req = urllib.request.Request(
        f"{GATEWAY}/api/v1/tickets/incidencias?zone={zone}",
        headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.load(resp)["data"]


def leer_verdad_campo(zone):
    ticket_ids = set()
    with open(VERDAD_CAMPO_CSV, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row["zone"] == zone:
                ticket_ids.add(row["ticket_id"])
    return ticket_ids


def analizar(zone):
    token = login()
    incidencias = obtener_incidencias(token, zone)
    verdad = leer_verdad_campo(zone)

    if not verdad:
        raise SystemExit(
            f"No hay filas de {VERDAD_CAMPO_CSV} para {zone} -- corra primero "
            f"experimentos/inyector_averias.py --zone {zone}")

    # La incidencia "de la averia" es la que mas tickets verdaderos concentra -- con c0 cada
    # ticket abre su propia incidencia, asi que ninguna concentra mas de 1; con c1/c2 deberia
    # haber una que domine si la averia se agrupo bien.
    mejor = max(incidencias, key=lambda inc: len(set(inc["ticketIds"]) & verdad), default=None)

    incidencias_efectivas = len(incidencias)
    if mejor is None or not set(mejor["ticketIds"]) & verdad:
        precision = exhaustividad = 0.0
        interseccion = 0
    else:
        conjunto_incidencia = set(mejor["ticketIds"])
        interseccion = len(conjunto_incidencia & verdad)
        precision = interseccion / len(conjunto_incidencia)
        exhaustividad = interseccion / len(verdad)

    print(f"=== Correlacion en {zone} ===")
    print(f"Modo de la incidencia elegida: {mejor['correlMode'] if mejor else 'N/A'}")
    print(f"Incidencias efectivas (a atender): {incidencias_efectivas}")
    print(f"Tickets de la averia (verdad de campo): {len(verdad)}")
    print(f"Tickets en la incidencia elegida: {len(mejor['ticketIds']) if mejor else 0}")
    print(f"Interseccion (correctos): {interseccion}")
    print(f"Precision del agrupamiento:     {precision:.2%}")
    print(f"Exhaustividad del agrupamiento: {exhaustividad:.2%}")
    return {
        "incidencias_efectivas": incidencias_efectivas,
        "precision": precision,
        "exhaustividad": exhaustividad,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--zone", required=True, choices=["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"])
    args = parser.parse_args()
    analizar(args.zone)
