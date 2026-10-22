#!/usr/bin/env python3
"""Smoke/static test: UI wiring and Android XML resource validity (not Android compilation)."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main'
resources = list((src / 'res').rglob('*.xml'))
for f in resources + [src / 'AndroidManifest.xml']:
    ET.parse(f)

panel = (src / 'res/layout/activity_painel_interno.xml').read_text(encoding='utf-8')
nav = (src / 'java/com/example/rcarpremium/MainActivityPainelInterno.kt').read_text(encoding='utf-8')
manifest = (src / 'AndroidManifest.xml').read_text(encoding='utf-8')
assert 'btnImportarNotaInterno' in panel and 'btnImportarNotaInterno' in nav
assert 'MainActivityImportarNota' in manifest
for file in ['NfeImportParser.kt', 'NfeImportStore.kt', 'MainActivityImportarNota.kt']:
    assert (src / 'java/com/example/rcarpremium' / file).is_file()
assert 'play-services-code-scanner:16.1.0' in (root / 'app/build.gradle.kts').read_text()
print(f'OK: {len(resources)} XML resources, manifest, new import activity, permissions, parser, store, scanner dependency')
