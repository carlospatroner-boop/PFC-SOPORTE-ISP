"""Inyector de averias (Adicion 2 de la Ampliacion del Modulo G, equipo ACC -- ver
docs/adr/0008-correl-incidencias.md). Provoca una averia simulada en una zona: crea tickets
reales (via la API, a traves del API Gateway, igual que un cliente real -- mismo criterio que
tests/load/locustfile.py) para N abonados simulados, envia telemetria EQUIPO real al canal de
PE-U1 (telemetry-service, puerto 9500) para esos mismos abonados, y deja constancia exacta de a
quien afecto en experimentos/resultados/verdad_campo.csv.

En un ISP real nadie sabe con certeza que abonados estaban afectados por una averia: se deduce
despues, con error. Aqui la averia la provoca este mismo guion, asi que esa lista se conoce con
exactitud -- es lo que permite calcular precision/exhaustividad del agrupamiento contra una
respuesta conocida, sin discusion posible (ver experimentos/analizar_correlacion.py).

Sin dependencias nuevas: solo la biblioteca estandar (urllib, socket, json, csv).

Uso (con el stack levantado via docker compose):
    python experimentos/inyector_averias.py --zone QUEVEDO_CENTRO --abonados 8 --severidad ALTA
"""
import argparse
import csv
import json
import os
import socket
import time
import urllib.error
import urllib.request

GATEWAY = "http://localhost:8000"
TELEMETRY_HOST = "localhost"
TELEMETRY_PORT = 9500
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


def crear_ticket(token, zone, abonado_id):
    payload = {
        "zone": zone,
        "title": f"Sin servicio (averia inyectada, abonado {abonado_id})",
        "description": "Generado por experimentos/inyector_averias.py -- verdad de campo conocida",
        "contactPhone": "0990000000",
        "address": f"Simulado -- {abonado_id}",
    }
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        f"{GATEWAY}/api/v1/tickets", data=body,
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {token}"},
        method="POST")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.load(resp)["data"]["ticketId"]
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"No se pudo crear el ticket para {abonado_id}: {e.code} {e.read()}") from e


def enviar_telemetria_equipo(zone, abonado_id, severidad):
    nivel_dbm = {"BAJA": -65, "MEDIA": -75, "ALTA": -88}.get(severidad, -75)
    mensaje = {
        "type": "EQUIPO",
        "originId": abonado_id,
        "zone": zone,
        "payload": {"signalLevelDbm": nivel_dbm, "severidad": severidad},
        "senderClock": 1,
    }
    with socket.create_connection((TELEMETRY_HOST, TELEMETRY_PORT), timeout=5) as s:
        s.sendall((json.dumps(mensaje) + "\n").encode("utf-8"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--zone", required=True, choices=["QUEVEDO_CENTRO", "QUEVEDO_NORTE", "QUEVEDO_SUR"])
    parser.add_argument("--abonados", type=int, default=5, help="Cantidad de abonados afectados")
    parser.add_argument("--severidad", choices=["BAJA", "MEDIA", "ALTA"], default="ALTA")
    parser.add_argument("--prefijo", default="ave", help="Prefijo del id de averia (para correr varias)")
    args = parser.parse_args()

    print(f"Inyectando averia en {args.zone}: {args.abonados} abonados, severidad {args.severidad}")
    token = login()

    filas = []
    for i in range(args.abonados):
        abonado_id = f"{args.prefijo}-abonado-{i}"
        instante = time.time()
        ticket_id = crear_ticket(token, args.zone, abonado_id)
        enviar_telemetria_equipo(args.zone, abonado_id, args.severidad)
        filas.append({
            "abonado_id": abonado_id, "ticket_id": ticket_id, "zone": args.zone,
            "instante": instante, "severidad": args.severidad,
        })
        print(f"  {abonado_id} -> ticket {ticket_id}")

    escribir_csv = not os.path.exists(VERDAD_CAMPO_CSV)
    os.makedirs(os.path.dirname(VERDAD_CAMPO_CSV), exist_ok=True)
    with open(VERDAD_CAMPO_CSV, "a", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["abonado_id", "ticket_id", "zone", "instante", "severidad"])
        if escribir_csv:
            writer.writeheader()
        writer.writerows(filas)

    print(f"Verdad de campo escrita/actualizada en {VERDAD_CAMPO_CSV} ({len(filas)} filas nuevas)")


if __name__ == "__main__":
    main()
