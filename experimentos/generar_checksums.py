# -*- coding: utf-8 -*-
"""
generar_checksums.py -- Sumas de verificacion (SHA-256) de los datos crudos experimentales.

Cubre las dos rutas de resultados que tiene el proyecto (ver README.md):
  - experimentos/resultados/  (campana CORREL: verdad de campo, corridas, analisis)
  - resultados/locust/        (campanas de carga: estadisticas, fallas, excepciones por corrida)

Se corre como ultimo paso despues de generar o regenerar cualquiera de los dos conjuntos de
datos (correr_campana.py lo invoca automaticamente al final de main(); tambien se puede correr
suelto despues de tocar resultados/locust/ a mano). Escribe un archivo SHA256SUMS.txt en la raiz
de cada carpeta, en el mismo formato que entiende `sha256sum -c` de coreutils.

Uso:
    python experimentos/generar_checksums.py
    # o, para verificar despues de clonar:
    cd experimentos/resultados && sha256sum -c SHA256SUMS.txt
    cd resultados/locust && sha256sum -c SHA256SUMS.txt
"""
import hashlib
from pathlib import Path

ROOT = Path(__file__).parent.parent
RUTAS = [
    ROOT / "experimentos" / "resultados",
    ROOT / "resultados" / "locust",
]


def sha256_de(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def generar_para(carpeta: Path):
    if not carpeta.exists():
        print(f"  (omitida, no existe: {carpeta})")
        return
    archivo_sumas = carpeta / "SHA256SUMS.txt"
    entradas = []
    for f in sorted(carpeta.rglob("*")):
        if f.is_file() and f.name != "SHA256SUMS.txt":
            digest = sha256_de(f)
            nombre_relativo = f.relative_to(carpeta).as_posix()
            entradas.append(f"{digest}  {nombre_relativo}")
    # newline="" fuerza LF real sin importar el SO -- en Windows, write_text() normal traduce
    # \n a \r\n, y sha256sum -c interpreta ese \r como parte del nombre de archivo (falla con
    # "No such file or directory" aunque el contenido sea correcto).
    with open(archivo_sumas, "w", encoding="utf-8", newline="") as f:
        f.write("\n".join(entradas) + "\n")
    print(f"  {archivo_sumas} -- {len(entradas)} archivos")


def main():
    print("Generando sumas de verificacion (SHA-256):")
    for carpeta in RUTAS:
        generar_para(carpeta)


if __name__ == "__main__":
    main()
