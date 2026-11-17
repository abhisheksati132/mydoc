"""Rebuild the audit inventory without treating automated scans as human review."""
import csv
import hashlib
import json
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = Path(__file__).resolve().parent
paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0')
coverage = {}
def record(path, status):
    if isinstance(status, dict):
        status = status.get('coverage', status.get('status', 'partial'))
    status = 'inventoried-only' if status in ('inventory-only', 'inventoried_only', 'third_party_inventory') else status
    ranks = {'inventoried-only': 0, 'partial': 1, 'full': 2}
    path = path.replace('\\', '/')
    if ranks.get(status, 0) >= ranks.get(coverage.get(path), -1):
        coverage[path] = status
for name in ('readers', 'ui', 'storage-coverage', 'root', 'viewer-extra', 'storage-tests', 'reader-tests', 'ui-tests', 'docs-config', 'ui-final', 'fixtures'):
    source = OUT / (name + '.json')
    if not source.exists():
        continue
    data = json.loads(source.read_text(encoding='utf-8-sig'))
    if name == 'storage-coverage' and isinstance(data, dict):
        for entry in data.get('production_files', []):
            record(entry['file'], entry.get('production_review', 'inventoried-only'))
        for entry in data.get('related_tests', []):
            record(entry.get('file', entry.get('path', '')), entry.get('review', 'inventoried-only'))
        continue
    if isinstance(data, list):
        for entry in data:
            record(entry['path'], entry.get('coverage', 'partial'))
    elif isinstance(data, dict):
        for path, status in data.items():
            record(path, status)

rows, xml_errors = [], []
for path in filter(None, paths):
    file = ROOT / path
    raw = file.read_bytes()
    try:
        text = raw.decode('utf-8')
        binary = '\0' in text
    except UnicodeDecodeError:
        text, binary = '', True
    if '/src/main/' in path and file.suffix in ('.kt', '.java', '.c', '.h'):
        category = 'production-source'
    elif '/src/test/' in path or '/src/androidTest/' in path:
        category = 'test'
    elif binary:
        category = 'binary'
    elif path.endswith('.xml'):
        category = 'xml-resource-or-config'
    else:
        category = 'configuration-or-documentation'
    status = coverage.get(path, 'inventoried-only')
    if isinstance(status, dict):
        status = status.get('coverage', status.get('status', 'partial'))
    if status in ('inventory-only', 'inventoried_only', 'third_party_inventory'):
        status = 'inventoried-only'
    xml_check = ''
    if file.suffix == '.xml' and not binary:
        try:
            ET.fromstring(raw)
            xml_check = 'well-formed'
        except ET.ParseError as error:
            xml_check = str(error)
            xml_errors.append({'path': path, 'error': str(error)})
    rows.append([path, category, status, len(raw), len(text.splitlines()), hashlib.sha256(raw).hexdigest(), xml_check])
with (OUT / 'FILE_COVERAGE.csv').open('w', newline='', encoding='utf-8') as stream:
    writer = csv.writer(stream)
    writer.writerow(['path', 'category', 'review_status', 'bytes', 'lines', 'sha256', 'xml_syntax_check'])
    writer.writerows(rows)
counts = {}
for row in rows:
    counts[row[2]] = counts.get(row[2], 0) + 1
summary = {'commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip(),
           'tracked_files': len(rows), 'review_counts': counts, 'xml_errors': xml_errors,
           'note': 'Inventory and XML syntax checks do not establish semantic review or functional correctness.'}
(OUT / 'coverage-summary.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
pending = [row[0] for row in rows if row[2] != 'full']
(OUT / 'REMAINING_FILES.txt').write_text('\n'.join(pending) + '\n', encoding='utf-8')
print(json.dumps(summary, indent=2))
