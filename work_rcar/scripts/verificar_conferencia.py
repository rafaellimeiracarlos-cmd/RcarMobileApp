#!/usr/bin/env python3
"""Verificações estáticas para o fluxo de conferência; não substitui compilação Android."""
from pathlib import Path
from xml.etree import ElementTree as ET

root = Path(__file__).resolve().parents[1] / "app" / "src" / "main"
src = root / "java" / "com" / "example" / "rcarpremium"
xmls = list(root.rglob("*.xml"))
for path in xmls:
    ET.parse(path)
manifest = (root / "AndroidManifest.xml").read_text()
layout = (root / "res" / "layout" / "activity_painel_interno.xml").read_text()
checks = {
    "activity registrada": 'MainActivityConferenciaEstoque' in manifest,
    "atalho presente": 'btnConferenciaEstoque' in layout and 'btnConferenciaEstoque' in (src / 'MainActivityPainelInterno.kt').read_text(),
    "estoque bloqueado antes da aprovação": 'saveConferenceApprovalAtomically' in (src / 'EstoqueConferenciaStore.kt').read_text(),
    "importação envia para conferência": 'EstoqueConferenciaStore.submitInvoice' in (src / 'MainActivityImportarNota.kt').read_text(),
    "alteração direta da tela de estoque removida": 'AdminLocalStore.adjustStock(this@MainActivityEstoque' not in (src / 'MainActivityEstoque.kt').read_text(),
    "função de devolução e reenvio": all(k in (src / 'EstoqueConferenciaStore.kt').read_text() for k in ('returnToEmployee', 'resubmitLines')),
    "registro de correções após aprovação": 'correctStock' in (src / 'EstoqueConferenciaStore.kt').read_text(),
    "permissões por funcionário": 'conferir_recebimento' in (src / 'UsuarioLocalRepository.kt').read_text(),
}
for name, passed in checks.items():
    print(("OK" if passed else "ERRO") + ": " + name)
print("XMLs bem formados:", len(xmls))
assert all(checks.values())
