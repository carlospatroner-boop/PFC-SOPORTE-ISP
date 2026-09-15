# Instantánea congelada — Entrega 4

Este directorio existe por el Entregable 26 de la Guía de Cierre ("Separación entre documento
vivo y documento congelado"): el PDF versionado en `docs/latex/main.pdf` **no** sirve como
instantánea, porque es la salida del documento vivo (`docs/latex/main.tex`, veinte inclusiones
que el equipo sigue editando) recompilada en cada `push` a `main` — no lleva fecha ni commit de
referencia, así que no hay forma de saber a qué versión corresponde un PDF descargado en un
momento dado.

## Qué es este archivo

[`manuscrito-entrega4-50024c4-2026-09-15.pdf`](manuscrito-entrega4-50024c4-2026-09-15.pdf) es una
copia congelada del manuscrito, generada por el mismo job `compile-latex` del pipeline
(`.github/workflows/ci-cd.yml`) a partir del commit `50024c4` (2026-09-15), el estado de `main`
inmediatamente después de fusionar el Entregable 22 (corrección de los conteos de pruebas).

**Esta copia no se vuelve a recompilar ni se sobrescribe.** Si aparecen más correcciones después
de este commit, el documento vivo (`docs/latex/main.tex` / `docs/latex/main.pdf`, y el PDF que
publica el [GitHub Release `mobile-release`](../../../../releases/tag/mobile-release), que sí se
actualiza en cada `push`) las reflejará; esta instantánea seguirá representando exactamente el
estado del manuscrito en el commit `50024c4`, para que cualquiera pueda citar una versión
específica y verificable en vez de "el PDF de hoy".

## Cómo verificarla

```bash
git show 50024c4:docs/latex/main.tex > /tmp/main_50024c4.tex   # el fuente en ese commit exacto
sha256sum manuscrito-entrega4-50024c4-2026-09-15.pdf            # huella del PDF congelado
```

El PDF se compiló con la misma imagen Docker (`texlive/texlive:latest`) y los mismos cuatro pasos
(`pdflatex`, `bibtex`, `pdflatex` × 2) que documenta la raíz del README — ver el job
`compile-latex` del run correspondiente al commit `50024c4` para el log de esa compilación
exacta.
