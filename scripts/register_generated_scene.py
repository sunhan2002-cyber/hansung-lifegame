"""Register a generated PNG and its provenance, leaving runtime acceptance pending."""
import argparse
import hashlib
import json
import shutil
from pathlib import Path
from validate_images import read_size

ROOT = Path(__file__).resolve().parents[1]
F = ROOT / 'docs/assignments/20261031'


def load(p):
    return json.loads(p.read_text(encoding='utf-8-sig'))


def write(p, value):
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('record', type=Path)
    args = parser.parse_args()
    entry = load(args.record)
    brief = load(F / '이미지제작_착수명세.json')
    g = next(g for g in brief['groups'] if entry['eventId'] in g['eventIds'])
    source = Path(entry['generatedPath'])
    dest = ROOT / 'app/src/main/assets/images' / g['targetAssetPath']
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    if dest.exists() and hashlib.sha256(dest.read_bytes()).hexdigest() != digest:
        raise ValueError('Existing scene differs: replacement must be reviewed explicitly')
    width, height = read_size(source)
    assert width * 3 == height * 4
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, dest)
    entry.update(groupId=g['groupId'], assignmentOwner=g['productionOwner'],
        actualProducer='김선한 담당 Codex (사용자 요청으로 대행)',
        assetPath=g['targetAssetPath'], sha256=digest, width=width, height=height,
        generationMethod='built-in image_gen; original PNG copied without resizing',
        status='PNG_PRODUCED_PENDING_VISUAL_AND_RUNTIME_REVIEW')
    manifest=F/'submissions/본제작_생성기록_20261105.json'
    record=load(manifest) if manifest.exists() else dict(date='2026-11-05',assets=[])
    record['assets']=[a for a in record['assets'] if a['eventId'] != entry['eventId']] + [entry]
    write(manifest,record)
    print(json.dumps(dict(eventId=entry['eventId'],assetPath=entry['assetPath'],sha256=digest),ensure_ascii=False))


if __name__ == '__main__':
    main()
