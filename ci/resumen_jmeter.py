#!/usr/bin/env python3
"""Convierte el statistics.json de cada dashboard HTML de JMeter en una tabla Markdown
para el resumen de la ejecucion de GitHub Actions ($GITHUB_STEP_SUMMARY).
Solo informa: no hace fallar el pipeline.

Uso: python3 ci/resumen_jmeter.py reportes/jmeter >> "$GITHUB_STEP_SUMMARY"
(busca reportes/jmeter/<plan>/html/statistics.json)
"""
import json
import sys
from pathlib import Path


def fila(nombre: str, s: dict) -> str:
    return (
        f"| {nombre} | {s['sampleCount']} | {s['errorCount']} | {s['errorPct']:.2f} % "
        f"| {s['meanResTime']:.0f} | {s['medianResTime']:.0f} | {s['pct2ResTime']:.0f} "
        f"| {s['maxResTime']:.0f} | {s['throughput']:.1f} |"
    )


def main() -> int:
    base = Path(sys.argv[1] if len(sys.argv) > 1 else "reportes/jmeter")
    archivos = sorted(base.glob("*/html/statistics.json"))
    if not archivos:
        print("### ⚠️ JMeter: no se generaron reportes\n")
        return 0

    cabecera = [
        "| Muestra | Peticiones | Errores | % error | Promedio (ms) | Mediana (ms) | p95 (ms) | Máx (ms) | Peticiones/s |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]

    print("### JMeter: resumen por plan\n")
    print("\n".join(cabecera))
    detalles = []
    for archivo in archivos:
        plan = archivo.parent.parent.name
        stats = json.loads(archivo.read_text(encoding="utf-8"))
        total = stats.pop("Total", None)
        if total:
            print(fila(f"**{plan}**", total))
        detalle = [f"<details><summary>{plan}: detalle por petición</summary>", ""] + cabecera
        for nombre, s in sorted(stats.items()):
            detalle.append(fila(nombre.replace("|", "\\|"), s))
        detalles += detalle + ["", "</details>", ""]
    print()
    print("\n".join(detalles))
    print("_El dashboard HTML completo de cada plan está en el artefacto **reporte-jmeter**._\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
