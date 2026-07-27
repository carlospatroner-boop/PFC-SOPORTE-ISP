"""
mock_server.py — servidor mock que imita el contrato REST real de ticket-service,
usado SOLO para probar que el frontend (app.js) renderiza correctamente antes de
tener el cluster CockroachDB levantado. No es parte del sistema de produccion.

Uso: python3 mock_server.py   (escucha en 0.0.0.0:8002, igual puerto que el real)
"""
import json
import re
import uuid
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

TICKETS = [
    {
        "zone": "QUEVEDO_CENTRO", "ticketId": str(uuid.uuid4()),
        "clientId": str(uuid.uuid4()), "technicianId": None,
        "category": None, "priority": None, "status": "NUEVO",
        "description": "Sin acceso a Internet -- El router muestra luz roja",
        "createdAt": datetime.now(timezone.utc).isoformat(),
        "slaDeadline": (datetime.now(timezone.utc) + timedelta(hours=24)).isoformat(),
        "slaBreached": False,
    },
    {
        "zone": "QUEVEDO_NORTE", "ticketId": str(uuid.uuid4()),
        "clientId": str(uuid.uuid4()), "technicianId": str(uuid.uuid4()),
        "category": "DNS", "priority": "ALTO", "status": "EN_PROGRESO",
        "description": "Lentitud extrema -- Velocidad menor a 1 Mbps",
        "createdAt": (datetime.now(timezone.utc) - timedelta(hours=5)).isoformat(),
        "slaDeadline": (datetime.now(timezone.utc) - timedelta(hours=1)).isoformat(),
        "slaBreached": True,
    },
    {
        "zone": "QUEVEDO_SUR", "ticketId": str(uuid.uuid4()),
        "clientId": str(uuid.uuid4()), "technicianId": str(uuid.uuid4()),
        "category": "HARDWARE", "priority": "CRITICO", "status": "RESUELTO",
        "description": "Corte total del servicio -- Fibra cortada en el poste",
        "createdAt": (datetime.now(timezone.utc) - timedelta(days=1)).isoformat(),
        "slaDeadline": (datetime.now(timezone.utc) - timedelta(hours=20)).isoformat(),
        "slaBreached": False,
    },
]


def envelope(data, message="OK"):
    return {"data": data, "message": message, "timestamp": datetime.now(timezone.utc).isoformat()}


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, status, payload):
        body = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "*")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self._send_json(200, {})

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/v1/tickets":
            qs = parse_qs(parsed.query)
            result = TICKETS
            if "zone" in qs:
                result = [t for t in result if t["zone"] == qs["zone"][0]]
            if "status" in qs:
                result = [t for t in result if t["status"] == qs["status"][0]]
            self._send_json(200, envelope(result))
            return
        self._send_json(404, envelope(None, "not found"))

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length) or b"{}")
        new_ticket = {
            "zone": body.get("zone"), "ticketId": str(uuid.uuid4()),
            "clientId": body.get("clientId"), "technicianId": None,
            "category": None, "priority": None, "status": "NUEVO",
            "description": f"{body.get('title','')} -- {body.get('description','')}",
            "createdAt": datetime.now(timezone.utc).isoformat(),
            "slaDeadline": (datetime.now(timezone.utc) + timedelta(hours=24)).isoformat(),
            "slaBreached": False,
        }
        TICKETS.append(new_ticket)
        self._send_json(201, envelope(new_ticket, "Ticket creado exitosamente"))

    def do_PATCH(self):
        m = re.match(r"^/api/v1/tickets/([^/]+)/([^/]+)/status$", self.path)
        if not m:
            self._send_json(404, envelope(None, "not found"))
            return
        zone, ticket_id = m.groups()
        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length) or b"{}")
        for t in TICKETS:
            if t["zone"] == zone and t["ticketId"] == ticket_id:
                t["status"] = body.get("status", t["status"])
                self._send_json(200, envelope(t, "Estado actualizado"))
                return
        self._send_json(404, envelope(None, "ticket no encontrado"))

    def log_message(self, format, *args):
        pass  # silenciar logs de acceso


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8002), Handler)
    print("Mock ticket-service escuchando en http://localhost:8002")
    server.serve_forever()
