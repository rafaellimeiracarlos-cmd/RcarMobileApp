"""Checagem estática rápida quando não há Gradle disponível."""
from pathlib import Path
import re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
app=root/'app/src/main'
manifest=ET.parse(app/'AndroidManifest.xml').getroot()
android='{http://schemas.android.com/apk/res/android}'
activities={x.attrib.get(android+'name','').split('.')[-1] for x in manifest.findall('.//activity')}
source=app/'java/com/example/rcarpremium'
assert {'MainActivityCentralNotas','MainActivityAcompanharPedido'}.issubset(activities)
for s in ['MainActivityCentralNotas','MainActivityAcompanharPedido']:
    assert (source/f'{s}.kt').is_file()
refs=set()
for f in source.glob('*.kt'):
    refs.update(re.findall(r'R\.id\.(\w+)', f.read_text()))
ids=set()
for f in (app/'res').rglob('*.xml'):
    rt=ET.parse(f)
    for element in rt.iter():
        idv=element.attrib.get(android+'id','')
        if idv.startswith('@+id/'): ids.add(idv.split('/')[-1])
print('Kotlin view references without layout IDs:',sorted(refs-ids))
assert not refs-ids, f"IDs sem layout: {refs-ids}"
assert 'lancar_notas' in (source/'UsuarioLocalRepository.kt').read_text()
assert 'Apenas o CEO pode' in (source/'ArquivoNotasStore.kt').read_text()
assert 'PENDENTE' in (source/'EstoqueConferenciaStore.kt').read_text()
assert 'android:grantUriPermissions="true"' in (app/'AndroidManifest.xml').read_text()
print('Manifest, XML, IDs e permissoes: OK')
