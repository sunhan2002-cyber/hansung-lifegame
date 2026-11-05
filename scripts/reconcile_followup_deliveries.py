"""Reconcile submitted art using explicit, hash-bound review records.

Does not approve missing images or infer approval from the presence of a PNG.
Run after adding a reviewed entry to submissions/통합_그림검토_20261105.json.
"""
import csv
import hashlib
import json
from pathlib import Path

from validate_followup_image_coverage import event_hash
from validate_images import read_size

ROOT = Path(__file__).resolve().parents[1]
FOLDER = ROOT / 'docs/assignments/20261031'
ASSETS = ROOT / 'app/src/main/assets'


def load(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def write(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def rows(path):
    with path.open(encoding='utf-8-sig', newline='') as f:
        return list(csv.DictReader(f))


def write_rows(path, values):
    with path.open('w', encoding='utf-8-sig', newline='') as f:
        w = csv.DictWriter(f, fieldnames=list(values[0]))
        w.writeheader()
        w.writerows(values)


def main():
    review_path = FOLDER / 'submissions/통합_그림검토_20261105.json'
    reviews = load(review_path)
    brief = load(FOLDER / '이미지제작_착수명세.json')
    ledger = rows(FOLDER / '이미지그룹_제작대장.csv')
    classification_path = FOLDER / 'submissions/김선한_분류합본.csv'
    classified = rows(classification_path)
    events = load(ASSETS / 'events.sample.json')
    by_event = {e['eventId']: e for e in events}
    catalog = load(ASSETS / 'image_ids.sample.json')
    catalog_by_id = {c['imageId']: c for c in catalog}
    routing = load(ASSETS / 'image_routing.json')
    by_image = {r['imageId']: r for r in routing['assets']}
    approved = load(FOLDER / '이미지그룹_승인manifest.json')
    by_group = {g['groupId']: g for g in approved}
    groups = {g['groupId']: g for g in brief['groups']}

    for review in reviews['assets']:
        group = groups[review['groupId']]
        path = review['assetPath']
        event = by_event[group['eventIds'][0]]
        image_id = event.get('povImageId') or event['imageId']
        group['targetImageId'] = image_id
        group['targetAssetPath'] = path
        png = ASSETS / 'images' / path
        digest = hashlib.sha256(png.read_bytes()).hexdigest()
        assert digest == review['sha256'], f'Stale review: {path}'
        width, height = read_size(png)
        assert width * 3 == height * 4
        c = catalog_by_id[image_id]
        c.update(fileName=Path(path).name, width=width, height=height)
        by_image[image_id] = dict(imageId=image_id, assetPath=path,
            usage=c['usage'], fileName=Path(path).name,
            fallbackText=c.get('fallbackText', '장면을 준비 중입니다'))
        done = all(review.get(k, {}).get('status') == 'PASS'
                   for k in ['semantic', 'visual', 'crop'])
        status = 'ACCEPTED_IN_INTEGRATION_BRANCH' if done else 'DELIVERED_PENDING_RUNTIME_REVIEW'
        approval = 'APPROVED' if done else 'PENDING'
        group.update(productionStatus=status, artApprovalStatus=approval,
            eventSourceHashes={e: event_hash(by_event[e]) for e in group['eventIds']})
        group['delivery'] = dict(assetPath=path, sha256=digest, width=width, height=height,
            actualProducer=review['actualProducer'], reviewEvidence=str(review_path.relative_to(ROOT)),
            deliveryStatus=status)
        for row in ledger:
            if row['groupId'] != group['groupId']:
                continue
            row.update(productionStatus=status, artApprovalStatus=approval,
                targetImageId=image_id, targetAssetPath=path,
                actualProducer=review['actualProducer'], deliveredAssetPath=path,
                deliveredSha256=digest, deliverySourcePr=str(review.get('sourcePr', 'integration')),
                deliverySourceCommit=review.get('sourceCommit', 'CURRENT_BRANCH_FILE_SHA256'))
        for row in classified:
            if row['eventId'] not in group['eventIds']:
                continue
            row.update(productionStatus=status)
            if done:
                row.update(approvedGroupId=group['groupId'], classificationStatus='APPROVED',
                    semanticReviewer=review['semantic']['reviewer'],
                    visualReviewer=review['visual']['reviewer'])
        if done:
            by_group[group['groupId']] = dict(groupId=group['groupId'], imageId=image_id,
                assetPath=path, sha256=digest, approvalStatus='APPROVED',
                eventIds=group['eventIds'], eventSourceHashes=group['eventSourceHashes'],
                semanticReviewer=review['semantic']['reviewer'],
                visualReviewer=review['visual']['reviewer'], cropReviewer=review['crop']['reviewer'],
                reviewEvidence=review, resolvedIssueEvidence={e: review['resolution'] for e in group['eventIds']})
        else:
            by_group.pop(group['groupId'], None)
    routing['assets'] = list(by_image.values())
    write(ASSETS / 'image_routing.json', routing)
    write(ASSETS / 'image_ids.sample.json', catalog)
    write(FOLDER / '이미지그룹_승인manifest.json', list(by_group.values()))
    write_rows(classification_path, classified)
    write_rows(FOLDER / '이미지그룹_제작대장.csv', ledger)
    production = [g for g in groups.values() if g['workType'] != 'REUSE']
    # Original unchanged reuse groups have no delivery and no production quota.
    production = [g for g in production if g['productionOwner'] in {'김선한','김우영','김재겸','신기훈','정원률'}]
    quotas = []
    for old in brief['quotas']:
        owned = [g for g in production if g['productionOwner'] == old['owner']]
        delivered = sum(bool(g.get('delivery')) for g in owned)
        quotas.append(dict(owner=old['owner'], total=len(owned), delivered=delivered,
                           remaining=len(owned)-delivered, pilotEventId=old['pilotEventId']))
    brief['progressAsOf'] = '2026-11-05'
    brief['quotas'] = quotas
    brief['phase'] = 'INTEGRATION_REVIEW_AND_PRODUCTION'
    brief['deliveryProgress'] = dict(deliveredGroups=sum(x['delivered'] for x in quotas),
        unproducedGroups=sum(x['remaining'] for x in quotas), quotas=quotas)
    for sample in brief['commonSamples']:
        matching = next((g for g in by_group.values() if sample['eventId'] in g['eventIds']), None)
        sample['status'] = 'APPROVED' if matching else 'PENDING'
    write(FOLDER / '이미지제작_착수명세.json', brief)
    print(json.dumps(brief['deliveryProgress'], ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
