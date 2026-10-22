"""Validação estática das integrações da tela de acompanhamento v3.4.5."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
src = root / 'app/src/main/java/com/example/rcarpremium'
app = root / 'app/src/main'
store = (src / 'PedidoLocalStore.kt').read_text(encoding='utf-8')
ui = (src / 'MainActivityAcompanharPedido.kt').read_text(encoding='utf-8')
assert 'val itens: List<PedidoProduto> = emptyList()' in store
assert 'itens = lerItens(item.optJSONArray("itens"))' in store
assert 'put("itens", JSONArray().apply' in store
assert 'itens = itens.filter { it.quantidade > 0 }' in store
for item in ['buildProducts(pedido)', 'VER PRODUTO COMPRADO', 'COMPRAR NOVAMENTE', 'prepararRecompra()', 'MainActivityItemCarrinho::class.java', 'NotaArquivo', 'RastreamentoInfo']:
    assert item in ui, f'Falta funcionalidade {item}'
manifest = ET.parse(app/'AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
activities = [x.get(android+'name', '') for x in manifest.findall('.//activity')]
for cl in ['MainActivityAcompanharPedido', 'MainActivityVerProdutos', 'MainActivityListaProdutos', 'MainActivityItemCarrinho']:
    assert any(a.endswith('.'+cl) for a in activities), f'Activity não registrada: {cl}'
for f in (app / 'res').rglob('*.xml'):
    ET.parse(f)
refs = set()
for f in src.glob('*.kt'):
    refs.update(re.findall(r'R\.id\.(\w+)', f.read_text(encoding='utf-8')))
ids = set()
for f in (app / 'res').rglob('*.xml'):
    for x in ET.parse(f).iter():
        v = x.get(android+'id','')
        if v.startswith('@+id/'): ids.add(v[5:])
assert not refs - ids, f'IDs faltando: {sorted(refs-ids)}'
print(f'OK: {len(list((app / "res").rglob("*.xml")))} XMLs válidos; Activities, IDs, histórico de itens e recompra conectados.')
