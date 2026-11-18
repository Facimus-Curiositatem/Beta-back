#!/usr/bin/env python3
"""Convierte los reportes JSON de newman (Postman) en una tabla Markdown para el
resumen de la ejecucion de GitHub Actions ($GITHUB_STEP_SUMMARY).

Uso: python3 ci/resumen_newman.py reportes/postman/*.json >> "$GITHUB_STEP_SUMMARY"
"""
import json
import sys
from pathlib import Path


def resumir(ruta: Path) -> list[str]:
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    run = datos["run"]
    stats = run["stats"]
    timings = run.get("timings", {})
    nombre = datos.get("collection", {}).get("info", {}).get("name", ruta.stem)

    req = stats["requests"]
    asserts = stats["assertions"]
    duracion_ms = timings.get("completed", 0) - timings.get("started", 0)
    estado = "✅" if asserts["failed"] == 0 and req["failed"] == 0 else "❌"

    lineas = [
        f"### {estado} Postman: {nombre}",
        "",
        "| Requests | Fallidos | Aserciones | Aserciones fallidas | Resp. promedio | Duración |",
        "|---:|---:|---:|---:|---:|---:|",
        f"| {req['total']} | {req['failed']} | {asserts['total']} | {asserts['failed']} "
        f"| {timings.get('responseAverage', 0):.0f} ms | {duracion_ms / 1000:.1f} s |",
        "",
    ]

    fallos = run.get("failures", [])
    if fallos:
        lineas += ["<details><summary>Fallos</summary>", "", "| Request | Prueba | Error |", "|---|---|---|"]
        for f in fallos[:50]:
            req_nombre = f.get("source", {}).get("name", "?")
            err = f.get("error", {})
            prueba = err.get("test", err.get("name", ""))
            msg = str(err.get("message", "")).replace("|", "\\|").replace("\n", " ")[:200]
            lineas.append(f"| {req_nombre} | {prueba} | {msg} |")
        lineas += ["", "</details>", ""]
    return lineas


def main() -> int:
    rutas = [Path(p) for p in sys.argv[1:] if Path(p).is_file()]
    if not rutas:
        print("### ⚠️ Postman: no se generaron reportes\n")
        return 0
    for ruta in rutas:
        print("\n".join(resumir(ruta)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
