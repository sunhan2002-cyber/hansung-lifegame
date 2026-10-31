"""Create the 2026-10-31 image classification baseline; never overwrite human reviews."""
import argparse
import csv
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
OWNERS = ['김선한', '김우영', '김재겸', '신기훈', '정원률']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out-dir', type=Path, default=ROOT / 'docs/assignments/20261031')
    args = parser.parse_args()
    args.out_dir.mkdir(parents=True, exist_ok=True)
    csv_path = args.out_dir / '사건이미지_분류대장.csv'
    summary_path = args.out_dir / '현황.json'
    if csv_path.exists() or summary_path.exists():
        parser.error('Existing inventory/review files are preserved. Choose a new --out-dir for another baseline.')
    assets = ROOT / 'app/src/main/assets'
    def load(name):
        return json.loads((assets / name).read_text(encoding='utf-8-sig'))
    events = load('events.sample.json')
    assert len({event['eventId'] for event in events}) == len(events)
    image_ids = {row['imageId']: row for row in load('image_ids.sample.json')}
    routing = load('image_routing.json')
    routed = {row['imageId']: row for row in routing['assets']}
    rows = []
    for event in sorted(events, key=lambda item: item['eventId']):
        image_id = event.get('povImageId') or event.get('imageId') or ''
        path = routed.get(image_id, {}).get('assetPath') or image_ids.get(image_id, {}).get('fileName', '')
        exists = bool(path) and (assets / 'images' / path).is_file()
        rows.append(dict(eventId=event['eventId'], title=event['title'], text=event['text'],
            periods='|'.join(event.get('periods', [])), kind=event.get('kind', ''),
            lifePath=event.get('lifePath', ''), perspective=event.get('view', ''),
            preferredImageId=image_id, resolvedAssetPath=path,
            preferredFileExists='YES' if exists else 'NO',
            classificationOwner='', place='', mainAction='', requiredObjects='',
            peopleAndAge='', timeOfDay='', approvedPerspective='', proposedGroupId='',
            existingArtVerdict='UNREVIEWED', reuseOrNewReason='', approvedGroupId='',
            semanticReviewer='', visualReviewer='', classificationStatus='UNREVIEWED',
            productionOwner='', productionStatus='NOT_ASSIGNED'))
    for exists in ('YES', 'NO'):
        for index, row in enumerate(item for item in rows if item['preferredFileExists'] == exists):
            row['classificationOwner'] = OWNERS[index % len(OWNERS)]
    with csv_path.open('w', encoding='utf-8-sig', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    summary = dict(
        asOf='2026-10-31', sourceCommit=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
        remoteMainAtAudit=subprocess.check_output(['git', 'rev-parse', 'origin/main'], cwd=ROOT, text=True).strip(),
        events=len(rows), choices=sum(len(event['choices']) for event in events),
        eventsWithPreferredFile=sum(row['preferredFileExists'] == 'YES' for row in rows),
        eventsMissingPreferredFile=sum(row['preferredFileExists'] == 'NO' for row in rows),
        pngFiles=len(list((assets / 'images').rglob('*.png'))),
        avatarPngFiles=len(list((assets / 'images/avatar_v4').rglob('*.png'))),
        classificationStatus='UNREVIEWED', approvedUniqueGroups=None,
        approvedReusableGroups=None, requiredProductionImages=None,
        productionQuantityStatus='PENDING_SEMANTIC_CLASSIFICATION',
        assignment={owner: dict(total=sum(row['classificationOwner'] == owner for row in rows),
            missingPreferredFile=sum(row['classificationOwner'] == owner and row['preferredFileExists'] == 'NO' for row in rows),
            existingFileReview=sum(row['classificationOwner'] == owner and row['preferredFileExists'] == 'YES' for row in rows)) for owner in OWNERS},
        scope='Static file presence and classification allocation only. No semantic match, decoding, or production quantity is certified.',
    )
    summary_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
