#!/usr/bin/env python3
"""Check resource duplicates, Vietnamese coverage and Android printf contracts."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

res = Path(__file__).resolve().parents[1] / 'app/src/main/res'
def read(folder):
    items = {}
    for path in sorted(folder.glob('*.xml')):
        for item in ET.parse(path).getroot():
            if item.tag not in ('string', 'plurals', 'string-array'):
                continue
            name = item.get('name')
            assert name not in items, f'Duplicate {name}: {path}'
            items[name] = item
    return items
base, vi = read(res / 'values'), read(res / 'values-vi')
missing = [name for name, item in base.items() if item.tag == 'string' and item.get('translatable') != 'false' and name not in vi]
assert not missing, f'Missing Vietnamese: {missing}'
# Positional placeholders may move in natural Vietnamese, but type/index must remain intact.
def formats(value):
    return sorted(re.findall(r'%(?:\d+\$)?[sdfoxegbcSDX]', value.replace('%%', '')))
for name, item in vi.items():
    original = base.get(name)
    if original is None or item.tag != original.tag:
        continue
    if item.tag == 'string':
        assert formats(''.join(item.itertext())) == formats(''.join(original.itertext())), f'Format mismatch: {name}'
    elif item.tag == 'plurals':
        by_quantity = {child.get('quantity'): child for child in original}
        for child in item:
            source = by_quantity.get(child.get('quantity'))
            if source is None: source = by_quantity['other']
            assert formats(child.text or '') == formats(source.text or ''), f'Plural mismatch: {name}'
assert not list((res / 'values-vi-rVN').glob('*.xml')), 'Stale regional overrides'
print(f'PASS: {len(base)} base resources, {len(vi)} Vietnamese resources; no duplicate names, missing strings or printf mismatches.')
