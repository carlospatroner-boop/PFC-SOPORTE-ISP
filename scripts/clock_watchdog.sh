#!/bin/bash
# Mantiene el reloj de la VM de Docker Desktop (WSL2) sincronizado con el host cada 15s.
# Necesario en esta maquina: el reloj de la VM se desincroniza varios segundos por minuto
# bajo carga (bug conocido de Hyper-V/WSL2), y CockroachDB rechaza consultas si el
# desfase entre nodos supera --max-offset (4500ms aqui, tope real de CockroachDB: <5s).
while true; do
  wsl -d docker-desktop -u root -- date -s "$(date -u '+%Y-%m-%d %H:%M:%S')" > /dev/null 2>&1
  sleep 15
done
