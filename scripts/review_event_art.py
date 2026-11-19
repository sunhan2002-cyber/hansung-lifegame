"""Build reproducible visual inspection sheets; never infer review verdicts."""
import hashlib
import json
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
OUT = ROOT / 'output/art-review-20261119'

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    events = json.loads((ASSETS/'events.sample.json').read_text(encoding='utf-8-sig'))
    routing = json.loads((ASSETS/'image_routing.json').read_text(encoding='utf-8-sig'))
    paths = {x['imageId']: x['assetPath'] for x in routing['assets']}
    legacy = {x['imageId']: x['fileName'] for x in json.loads((ASSETS/'image_ids.sample.json').read_text(encoding='utf-8-sig'))}
    font = ImageFont.truetype('C:/Windows/Fonts/malgun.ttf', 17)
    titlefont = ImageFont.truetype('C:/Windows/Fonts/malgunbd.ttf', 20)
    index = []
    for i, event in enumerate(events):
        image_id = event.get('povImageId') or event.get('imageId')
        path = paths.get(image_id) or legacy.get(image_id, '')
        file = ASSETS/'images'/path
        item = dict(index=i, sheet=i//16+1, eventId=event['eventId'], title=event['title'], text=event['text'], imageId=image_id, assetPath=path, exists=file.is_file())
        if file.is_file():
            item['sha256'] = hashlib.sha256(file.read_bytes()).hexdigest()
        index.append(item)
    for start in range(0, len(index), 16):
        sheet = Image.new('RGB', (2200, 1800), '#eeeeee')
        draw = ImageDraw.Draw(sheet)
        for j, item in enumerate(index[start:start+16]):
            x,y = (j%4)*550, (j//4)*450
            draw.text((x+8,y+4), f"{item['index']} {item['eventId']} {item['title']}", font=titlefont, fill='black')
            if item['exists']:
                with Image.open(ASSETS/'images'/item['assetPath']) as original:
                    original.load()
                    full = ImageOps.contain(original.convert('RGB'), (340,255))
                    sheet.paste(full,(x+5,y+36))
                    # 360x800 centre-fill: retained x=33.125..66.875%.
                    crop = ImageOps.fit(original.convert('RGB'), (115,255), centering=(.5,.5))
                    sheet.paste(crop,(x+360,y+36))
            else:
                draw.text((x+10,y+100), 'MISSING PREFERRED PNG', font=titlefont, fill='red')
            text = item['text']
            lines=[]
            while text:
                n=1
                while n<len(text) and draw.textlength(text[:n+1],font=font)<525: n+=1
                lines.append(text[:n]); text=text[n:]
            for k,line in enumerate(lines[:7]): draw.text((x+8,y+299+k*20),line,font=font,fill='black')
            draw.line((x,y+449,x+550,y+449),fill='#777777')
        sheet.save(OUT/f'sheet-{start//16+1:02}.jpg',quality=93)
    (OUT/'index.json').write_text(json.dumps(index,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(events=len(index),exists=sum(x['exists'] for x in index),missing=[x['eventId'] for x in index if not x['exists']],sheets=(len(index)+15)//16)))

if __name__ == '__main__': main()
