"""Write a factual manifest of routed scene IDs and independent avatar files."""
from pathlib import Path
import json,hashlib,subprocess
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'app/src/main/assets';D=ROOT/'docs/week10'
def main():
    routing=json.loads((A/'image_routing.json').read_text(encoding='utf8'))
    catalog=json.loads((A/'image_ids.sample.json').read_text(encoding='utf8'))
    events=json.loads((A/'events.sample.json').read_text(encoding='utf8'))
    scenes=json.loads((D/'week10_scene_manifest.json').read_text(encoding='utf8'))
    prior=json.loads(subprocess.check_output(['git','show','origin/main:app/src/main/assets/events.sample.json'],cwd=ROOT))
    old={e['eventId'] for e in prior};new=[e for e in events if e['eventId'] not in old]
    direct={e['key']:e for e in scenes}
    lines=['# 실제 이미지 manifest','',
        f'2026-10-29. 이미지 카탈로그 **{len(catalog)} ID**, 실제 라우팅 그림 **{len(routing["assets"])}장**(기존 21 + 신규 장면 24). 성장 몸 18개와 외형 파생 PNG는 장면 수에 포함하지 않는다.',
        '', '## 장면 24장',
        '', '일반 배경 6, 경로 사건 14(7경로 각각 2), 종료 대체 4. 24개 SHA-256이 모두 다르다. 이번 재작업에서 유산 분배(path_wea_09)·시험과 취미(path_hap_10) 두 장을 최신 사건 문맥에 맞춰 교체했다. 교체 전 그림은 수량에서 제외한다.',
        '', '출처: 내장 OpenAI 이미지 생성·편집 도구. 외부 사진·사용자 사진은 입력하지 않았다. 원본·프롬프트·변경 이력은 generated_scene_sources.json, 파일 해시는 week10_scene_manifest.json에 있다. 원본은 output/week10/originals/, 앱 파일은 아래 경로에 있다. 경로는 저장소 루트 기준.',
        '', '|imageId|실제 앱 파일|사건/용도|크기|중복|출처|','|---|---|---|---|---|---|']
    titles={e['eventId']:e['title'] for e in events}
    for x in scenes:
        use=titles.get(x['key'],x['key'])
        lines.append(f'|{x["imageId"]}|app/src/main/assets/images/{x["assetPath"]}|{x["key"]}: {use}|300×225|없음|imagegen|')
    lines+=['','## 최신 main 대비 신규 사건 매핑','',
        f'현재 콘텐츠 352사건·1009선택지·66결말. origin/main 대비 신규 사건은 {len(new)}개이며, 아래에서 전용과 테마 대체를 구분한다. 대체를 전용 그림 수로 가산하지 않는다.',
        '', '후보 순서: 명시 사건 imageId/기존 파일명 → 사건에 맞는 스탯 테마 → 시기 배경 → 기존 시기 초상 → placeholder. 기본 초상 6 ID와 스탯 7 ID를 보존했다. 종료 대체는 endingScenes의 achievement/relationship/peace/health이며 특정 학교·직업·가족 수를 사실로 확정하는 사진이 아니다.',
        '', '|사건 ID|실제 제목|적용|','|---|---|---|']
    for e in new:
        tags=routing.get('eventStatTags',{}).get(e['eventId'],[])
        use='전용: '+direct[e['eventId']]['imageId'] if e['eventId'] in direct else '테마 대체: '+(', '.join(tags) or '시기 배경 → 초상 → placeholder')
        lines.append(f'|{e["eventId"]}|{e["title"]}|{use}|')
    lines+=['','## 캐릭터 소재','',
        '현재 앱은 avatar_v4만 사용한다. **몸 18 + 피부/얼굴/노출 피부 54 + 눈 18 + 머리 24 + 얼굴 예제 시트 1 = PNG 115개**다. 이는 고유 삽화 115개라는 뜻이 아니다. 몸의 독립 생성 원본은 18개이며, 얼굴 바탕 6·머리 분리 편집 4·눈 분리 편집 3을 사용한다. 얼굴 시트는 기본 외형 합성 예제로 실제 사진 생성 결과가 아니다.',
        '', '제작·분리 편집은 imagegen, 자르기·최근접 배치·피부와 백발의 팔레트 치환은 package_natural_avatars.py다. 얼굴+머리 통합 PNG 교체 방식은 최종 앱에서 제외했다. 레이어와 ID 계약은 avatar_asset_contract.md, 원본/프롬프트는 avatar_v4_sources.json(deliveredLayerSource=true)으로 추적한다.',
        '', '|실제 파일|imageId/선택 키|용도|크기|SHA-256 중복|출처|','|---|---|---|---|---|---|']
    seen={}
    for f in sorted((A/'images/avatar_v4').rglob('*.png')):
        sha=hashlib.sha256(f.read_bytes()).hexdigest();dup=seen.get(sha,'없음');seen[sha]=f.name
        w,h=Image.open(f).size
        kind='avatar_body' if f.parent.name=='body' else 'example' if f.parent.name=='examples' else 'avatar_skin' if f.name.startswith('skin_') else 'avatar_eyes' if f.name.startswith('eyes_') else 'avatar_hair'
        lines.append(f'|{f.relative_to(ROOT).as_posix()}|{kind}: {f.stem}|분리 레이어/팔레트 파생' + ('/규격 예제' if kind=='example' else '')+f'|{w}×{h}|{dup}|생성 원본의 분리·배치|')
    (D/'image_manifest.md').write_text('\n'.join(lines)+'\n',encoding='utf8')
    print('Manifest:',len(catalog),'IDs,',len(routing['assets']),'routed pictures,',len(new),'new events')
if __name__=='__main__':main()
