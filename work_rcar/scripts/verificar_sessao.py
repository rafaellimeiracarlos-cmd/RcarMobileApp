#!/usr/bin/env python3
"""Verificação estrutural offline da sessão persistente (não substitui testes Android)."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[1]
k=r/'app/src/main/java/com/example/rcarpremium'
files={p.name:p.read_text(encoding='utf-8') for p in k.glob('*.kt')}
for p in (r/'app/src/main').rglob('*.xml'):
    ET.parse(p)
assert 'MainActivityPerfilInterno' in (r/'app/src/main/AndroidManifest.xml').read_text()
assert 'SessaoRcar.confirmarSaida(this)' in files['MainActivityHome.kt']
assert 'SessaoRcar.confirmarSaida(this)' in files['MainActivityPerfilInterno.kt']
assert 'MainActivityPerfilInterno::class.java' in files['BottomNav.kt']
assert 'SessaoRcar.atual(activity)' in files['BottomNav.kt']
assert 'val conta = SessaoRcar.atual(this)' in files['MainActivitycomprasonline.kt']
assert 'fun onResume()' in files['MainActivitycomprasonline.kt']
assert 'CART_PREFIX' in files['RcarLocalStore.kt']
assert 'if (!SessaoRcar.cliente(this))' in files['MainActivityPagamento.kt']
# Saída voluntária em um só fluxo, chamado a partir de perfis; não apagar sessão em navegação.
logout_callers=[name for name,txt in files.items() if re.search(r'RcarLocalStore\.logout\(this\)|RcarLocalStore\.logout\(activity\)',txt)]
assert logout_callers == ['SessaoRcar.kt'], logout_callers
assert 'RcarLocalStore.logout(this)' not in files['MainActivityHome.kt']
assert 'RcarLocalStore.logout(this)' not in files['MainActivityPainelInterno.kt']
print('OK — arquivos XML, navegação por perfil, sessão persistente e logout centralizado.')
