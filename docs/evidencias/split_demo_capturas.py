# -*- coding: utf-8 -*-
"""Divide la captura compuesta captura_demo_ticket_notificacion.png (real, capturada en
vivo el 26/07/2026) en 2 capturas independientes -- misma evidencia, sin volver a
ejecutar los servicios -- para cumplir con la observacion de la rubrica de mostrar
una secuencia de pasos en vez de una sola imagen combinada.
"""
import os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "captura_demo_ticket_notificacion.png")

im = Image.open(SRC)
W, H = im.size  # 2200 x 1040

TITLE_BAR = (0, 0, W, 60)

# Paso 1: creacion del ticket (POST + HTTP 201) -- ya incluye la barra de titulo.
paso1_box = (0, 0, W, 462)
paso1 = im.crop(paso1_box)
paso1.save(os.path.join(HERE, "captura_demo_paso1_creacion.png"))

# Paso 3: consulta de la notificacion (GET + HTTP 200) -- se compone la barra de
# titulo + el tramo inferior real de la misma captura, para que se vea como una
# ventana de terminal independiente.
get_section_box = (0, 649, W, 1017)
get_section = im.crop(get_section_box)
paso3 = Image.new("RGB", (W, TITLE_BAR[3] + get_section.height), "white")
paso3.paste(im.crop(TITLE_BAR), (0, 0))
paso3.paste(get_section, (0, TITLE_BAR[3]))
paso3.save(os.path.join(HERE, "captura_demo_paso3_notificacion.png"))

print("paso1:", paso1.size)
print("paso3:", paso3.size)
