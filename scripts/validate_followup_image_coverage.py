"""Audit event classification and approved art separately from fallback safety.

Default mode validates submitted classification structure. --acceptance additionally
requires reviewed groups, current event fingerprints, structurally valid PNGs and full coverage.
"""
import argparse
import collections
import csv
import hashlib
import json
from pathlib import Path
from validate_images import read_size, safe_path

ROOT = Path(__file__).resolve().parents[1]
FOLDER = Path('docs/assignments/20261031')
INPUT_FIELDS = ['eventId','title','text','periods','kind','lifePath','perspective',
                'preferredImageId','resolvedAssetPath','preferredFileExists','classificationOwner']
CLASS_FIELDS = ['place','mainAction','requiredObjects','peopleAndAge','timeOfDay',
                'approvedPerspective','proposedGroupId','existingArtVerdict','reuseOrNewReason']


def event_hash(event):
    # Choices can change what a pre-choice illustration inadvertently reveals.
    return hashlib.sha256(json.dumps(event, ensure_ascii=False, sort_keys=True,
        separators=(',', ':')).encode('utf8')).hexdigest()


def audit(events, baseline, rows, routing, catalog, images, groups=None):
    errors, pending = [], []
    events_by_id = {e['eventId']:e for e in events}
    base = {r['eventId']:r for r in baseline}
    counts = collections.Counter(r.get('eventId','') for r in rows)
    for key, count in counts.items():
        if count != 1: errors.append(f'duplicate classification: {key}')
        if key not in events_by_id: errors.append(f'unknown classification: {key}')
    for key in events_by_id.keys()-counts.keys(): errors.append(f'missing classification: {key}')
    assets = {x['imageId']:x['assetPath'] for x in routing['assets']}
    legacy = {x['imageId']:x.get('fileName','') for x in catalog}
    group_counts = collections.Counter(g.get('groupId','') for g in (groups or []))
    for key,count in group_counts.items():
        if not key or count != 1: errors.append(f'invalid/duplicate group: {key}')
    by_group = {g['groupId']:g for g in (groups or []) if g.get('groupId')}
    approved = set()
    for group_id,g in by_group.items():
        if g.get('approvalStatus') != 'APPROVED': continue
        path = g.get('assetPath','')
        if not safe_path(path):
            errors.append(f'unsafe approved image path: {group_id}')
            continue
        if not g.get('semanticReviewer') or not g.get('visualReviewer'):
            errors.append(f'missing group cross review: {group_id}')
            continue
        if read_size(images/path) is None:
            errors.append(f'missing/corrupt approved PNG: {group_id}:{path}')
            continue
        if g.get('sha256') != hashlib.sha256((images/path).read_bytes()).hexdigest():
            errors.append(f'approved PNG fingerprint missing/stale: {group_id}')
            continue
        if assets.get(g.get('imageId')) != path:
            errors.append(f'approved group is not routed: {group_id}')
            continue
        generic_ids = set(routing.get('statScenes',{}).values()) | set(routing.get('periodScenes',{}).values()) | set(routing.get('periodPortraits',{}).values())
        if g.get('imageId') in generic_ids:
            errors.append(f'generic fallback is not dedicated event completion: {group_id}')
            continue
        # Shared images are allowed only with a separate reviewed membership per event.
        members = g.get('eventIds',[])
        if not members or len(members) != len(set(members)):
            errors.append(f'empty/duplicate group membership: {group_id}')
            continue
        approved.add(group_id)
    preferred_present = []
    approved_events = []
    for row in rows:
        key = row.get('eventId','')
        if key not in events_by_id or key not in base: continue
        for field in INPUT_FIELDS:
            if row.get(field,'') != base[key].get(field,''): errors.append(f'changed input: {key}:{field}')
        for field in CLASS_FIELDS:
            if not str(row.get(field,'')).strip(): errors.append(f'empty classification: {key}:{field}')
        event = events_by_id[key]
        if row.get('title') != event['title'] or row.get('text') != event['text']:
            errors.append(f'stale event text: {key}')
        image_id = event.get('povImageId') or event.get('imageId') or ''
        if row.get('preferredImageId') != image_id: errors.append(f'stale preferred image ID: {key}')
        path = assets.get(image_id) or legacy.get(image_id)
        if path and safe_path(path) and read_size(images/path) is not None:
            preferred_present.append(key)
        group_id = row.get('approvedGroupId','')
        group = by_group.get(group_id,{})
        reasons = []
        if row.get('classificationStatus') != 'APPROVED': reasons.append('classification not approved')
        if not row.get('semanticReviewer'): reasons.append('semantic cross review pending')
        if not row.get('visualReviewer'): reasons.append('visual cross review pending')
        if group_id not in approved: reasons.append('approved routed PNG group missing')
        if key not in group.get('eventIds',[]): reasons.append('event/group membership missing')
        if group.get('eventSourceHashes',{}).get(key) != event_hash(event): reasons.append('current event fingerprint missing/stale')
        if group.get('imageId') != image_id: reasons.append('event does not select approved image ID')
        if row.get('existingArtVerdict') in {'REVISE_REQUIRED','REPLACE_REQUIRED','UNFIT_PERSPECTIVE'}:
            if not group.get('resolvedIssueEvidence',{}).get(key): reasons.append('image revision unresolved')
        if reasons: pending.append({'eventId':key,'reasons':reasons})
        else: approved_events.append(key)
    return dict(errors=errors,acceptancePending=pending,
        eventSourceHashes={key:event_hash(event) for key,event in events_by_id.items()},metrics=dict(
        events=len(events),classifiedRows=len(rows),uniqueClassifiedIds=len(counts),
        classificationByOwner=dict(collections.Counter(r.get('classificationOwner','') for r in rows)),
        proposedUniqueGroups=len({r.get('proposedGroupId') for r in rows if r.get('proposedGroupId')}),
        preferredFileEvents=len(set(preferred_present)),fallbackNeededEvents=len(events)-len(set(preferred_present)),
        approvedSemanticEvents=len(set(approved_events)),approvedSemanticCoverage=len(set(approved_events))/len(events) if events else 0,
        approvedRoutedGroups=len(approved)),
        scope='Classification validity and explicitly approved event art. Fallback availability is not semantic completion.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root',type=Path,default=ROOT)
    parser.add_argument('--classification',type=Path)
    parser.add_argument('--groups',type=Path)
    parser.add_argument('--report',type=Path,default=ROOT/'output/followup-sunhan/coverage.json')
    parser.add_argument('--acceptance',action='store_true')
    args = parser.parse_args()
    def load(path): return json.loads(path.read_text(encoding='utf-8-sig'))
    def csv_rows(path): return list(csv.DictReader(path.open(encoding='utf-8-sig',newline='')))
    assets=args.root/'app/src/main/assets'
    rows=args.classification or args.root/FOLDER/'submissions/김선한_분류합본.csv'
    group_path=args.groups or args.root/FOLDER/'이미지그룹_승인manifest.json'
    try:
        report=audit(load(assets/'events.sample.json'),csv_rows(args.root/FOLDER/'사건이미지_분류대장.csv'),
            csv_rows(rows),load(assets/'image_routing.json'),load(assets/'image_ids.sample.json'),
            assets/'images',load(group_path) if args.groups or group_path.exists() else None)
    except (OSError,ValueError,KeyError,TypeError) as exc:
        report=dict(errors=[str(exc)],acceptancePending=[],metrics={})
    args.report.parent.mkdir(parents=True,exist_ok=True)
    args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps(dict(metrics=report['metrics'],errors=report['errors'],
        pendingEvents=len(report['acceptancePending'])),ensure_ascii=False,indent=2))
    return int(bool(report['errors'] or (args.acceptance and report['acceptancePending'])))


if __name__=='__main__': raise SystemExit(main())
