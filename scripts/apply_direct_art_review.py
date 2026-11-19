"""Apply the explicitly inspected 2026-11-19 verdicts with current fingerprints."""
import csv
import hashlib
import json
from collections import Counter
from pathlib import Path

from PIL import Image
from validate_followup_image_coverage import event_hash

ROOT = Path(__file__).resolve().parents[1]
FOLDER = ROOT/'docs/assignments/20261031'
DEST = ROOT/'docs/assignments/20261113/submissions'
ASSETS = ROOT/'app/src/main/assets'

def load(path): return json.loads(path.read_text(encoding='utf-8-sig'))
def write(path, value): path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def readcsv(path):
    with path.open(encoding='utf-8-sig',newline='') as f: return list(csv.DictReader(f))
def writecsv(path, rows):
    with path.open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(rows[0])); writer.writeheader(); writer.writerows(rows)

def main():
    decisions=load(DEST/'김선한_직접그림검수_20261119_판정.json')
    inspected=load(ROOT/'output/art-review-20261119/index.json')
    prior_path=DEST/'김선한_직접그림검수_20261119.json'
    if prior_path.exists():
        prior={r['eventId']:r for r in load(prior_path)['events']}
        for item in inspected:
            assert item['eventId'] in prior, 'new event requires fresh visual inspection'
            assert item.get('sha256')==prior[item['eventId']].get('sha256'), 'PNG changed; inspect again before approval'
    events=load(ASSETS/'events.sample.json')
    event_by_id={e['eventId']:e for e in events}
    rows_path=FOLDER/'submissions/김선한_분류합본.csv'
    rows=readcsv(rows_path); by_id={r['eventId']:r for r in rows}
    manifest_path=FOLDER/'이미지그룹_승인manifest.json'
    # Every current event is reinspected; the previous approval set remains in git d345951.
    # Keep a single current approval per event instead of counting historic groups twice.
    groups=[]
    results=[]
    for item in inspected:
        event=event_by_id[item['eventId']]
        assert event['title']==item['title'] and event['text']==item['text'], 'event changed after inspection'
        reason=decisions['revisionByIndex'].get(str(item['index']))
        status='REVISION_REQUIRED' if reason else 'PASS'
        if not item['exists']: status='MISSING_PNG'; reason='현재 사건에 지정된 PNG 파일이 존재하지 않음'
        file=ASSETS/'images'/item['assetPath']
        if item['exists']:
            assert hashlib.sha256(file.read_bytes()).hexdigest()==item['sha256'], item['eventId']+' changed after inspection'
            with Image.open(file) as im:
                im.load(); width,height=im.size
                assert width*3==height*4
        row=by_id[item['eventId']]
        result=dict(eventId=item['eventId'],title=item['title'],imageId=item['imageId'],assetPath=item['assetPath'],
            sha256=item.get('sha256'),eventSourceHash=event_hash(event),status=status,
            reviewer=decisions['reviewer'],reason=reason or '본문의 장소·인물·행동을 원본과 대조하고 360×800 중앙 크롭의 상황 식별 가능성을 직접 확인',
            expectedAction=row.get('mainAction'),requiredObjects=row.get('requiredObjects'),
            sheet=f"output/art-review-20261119/sheet-{item['sheet']:02}.jpg",cell=item['index']%16,
            cropMethod='ImageOps.fit centre-fill 360:800; simulated crop inspected directly, not an emulator capture',
            visualMethod='원본 도트풍·색감·장면 파손·명백한 신체/사물 오류 직접 확인')
        results.append(result)
        if status=='PASS':
            gid='direct_20261119_'+item['eventId']
            row.update(approvedGroupId=gid,classificationStatus='APPROVED',semanticReviewer=decisions['reviewer'],visualReviewer=decisions['reviewer'])
            groups.append(dict(groupId=gid,imageId=item['imageId'],assetPath=item['assetPath'],sha256=item['sha256'],approvalStatus='APPROVED',
                eventIds=[item['eventId']],eventSourceHashes={item['eventId']:event_hash(event)},semanticReviewer=decisions['reviewer'],visualReviewer=decisions['reviewer'],
                cropReviewer=decisions['reviewer']+' (중앙 크롭 계산 재현 검수)',reviewEvidence=result,resolvedIssueEvidence={item['eventId']:result['reason']}))
        else:
            row.update(classificationStatus=status,semanticReviewer=decisions['reviewer'],visualReviewer=decisions['reviewer'])
    rejected={r['eventId'] for r in results if r['status']!='PASS'}
    for group in groups:
        if group.get('approvalStatus')=='APPROVED' and rejected.intersection(group.get('eventIds',[])):
            group['approvalStatus']='REVISION_REQUIRED'
            group['latestReviewEvidence']='docs/assignments/20261113/submissions/김선한_직접그림검수_20261119.json'
    counts=dict(Counter(r['status'] for r in results))
    write(DEST/'김선한_직접그림검수_20261119.json',dict(date=decisions['date'],method=decisions['method'],counts=counts,events=results))
    write(manifest_path,groups); writecsv(rows_path,rows)
    # Correct old progress counters that confused missing delivery metadata with missing files.
    result_by_id={r['eventId']:r for r in results}
    brief_path=FOLDER/'이미지제작_착수명세.json'
    brief=load(brief_path)
    for group in brief['groups']:
        members=[result_by_id[e] for e in group['eventIds'] if e in result_by_id]
        present=bool(members) and all(r['sha256'] for r in members)
        accepted=bool(members) and all(r['status']=='PASS' for r in members)
        group['productionStatus']='PNG_PRESENT' if present else 'PNG_MISSING'
        group['artApprovalStatus']='APPROVED' if accepted else 'REVISION_REQUIRED' if present else 'PENDING'
        group['latestReviewEvidence']='docs/assignments/20261113/submissions/김선한_직접그림검수_20261119.json'
    for quota in brief['quotas']:
        owned=[g for g in brief['groups'] if g.get('productionOwner')==quota['owner'] and g.get('workType')!='REUSE']
        present=sum(g['productionStatus']=='PNG_PRESENT' for g in owned)
        quota.update(total=len(owned),delivered=present,remaining=len(owned)-present,
            approvedGroups=sum(g['artApprovalStatus']=='APPROVED' for g in owned),
            revisionRequiredGroups=sum(g['artApprovalStatus']=='REVISION_REQUIRED' for g in owned))
    brief['progressAsOf']='2026-11-19'
    brief['deliveryProgress']=dict(deliveredGroups=sum(q['delivered'] for q in brief['quotas']),
        unproducedGroups=sum(q['remaining'] for q in brief['quotas']),quotas=brief['quotas'],
        scope='실제 연결 PNG 존재 기준; delivered는 의미·화풍·크롭 승인과 별개')
    write(brief_path,brief)
    ledger_path=FOLDER/'이미지그룹_제작대장.csv'
    ledger=readcsv(ledger_path); brief_by_group={g['groupId']:g for g in brief['groups']}
    for row in ledger:
        group=brief_by_group.get(row['groupId'])
        if group: row.update(productionStatus=group['productionStatus'],artApprovalStatus=group['artApprovalStatus'])
    writecsv(ledger_path,ledger)
    # Preserve the historical production ledger; publish actual file availability separately.
    write(DEST/'김선한_제작여부_정정_20261119.json',dict(events=len(results),pngPresent=sum(r['status']!='MISSING_PNG' for r in results),
        pngMissing=sum(r['status']=='MISSING_PNG' for r in results),reviewCounts=counts,
        correction='이전 delivered 메타데이터의 부재를 미제작으로 해석한 290개 보고는 철회. 현재 파일 352건 존재, 20건 미존재. 검수 미통과와 미제작은 별개.',
        missing=[r['eventId'] for r in results if r['status']=='MISSING_PNG']))
    report=['# 사건 이미지 직접 검수 결과 — 2026-11-19','',
        '| 구분 | 사건 수 |','| --- | ---: |',
        '| 지정 PNG 존재 | 352 |','| 지정 PNG 없음 | 20 |',
        f"| 직접 검수 통과·승인 반영 | {counts.get('PASS',0)} |",
        f"| 제작된 그림 수정 필요 | {counts.get('REVISION_REQUIRED',0)} |",'',
        '검수자: 김선한 담당 Codex. 사용자 지시에 따라 직접 대행 검수했다. 기존 팀원의 새 승인 서명을 대신 기록하지 않았다.',
        '현재 사건 본문, 실제 연결 PNG, 360×800 중앙 크롭 계산 재현을 대조했다. PNG·사건 SHA가 변경되면 해당 판정은 다시 확인해야 한다.',
        '이전 보고의 “290개 미제작·미검수” 수치는 제작대장의 delivery 필드를 실제 파일 부재로 잘못 해석한 값이다. 파일 존재와 검수 결과는 위 표를 따른다.',
        '에뮬레이터 대표 경로 검증은 별도 실행 결과로 기록한다. 이 표의 크롭 판정은 접촉시트 직접 검수다.','',
        '| 사건 ID | 제목 | 판정 | 검수 근거·수정 사유 |','| --- | --- | --- | --- |']
    for result in results:
        report.append('| '+' | '.join(str(result[k]).replace('|','/').replace('\n',' ') for k in ('eventId','title','status','reason'))+' |')
    (DEST/'김선한_직접그림검수_20261119.md').write_text('\n'.join(report)+'\n',encoding='utf-8')
    print(json.dumps(counts))

if __name__=='__main__': main()
