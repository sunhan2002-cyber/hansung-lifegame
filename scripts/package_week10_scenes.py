"""Package independently generated scenes; only nearest-neighbor resizing, no art synthesis."""
import json, hashlib, shutil
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'app/src/main/assets'
DOC=ROOT/'docs/week10'
def main():
    sources=json.loads((DOC/'generated_scene_sources.json').read_text(encoding='utf8'))
    routing=json.loads((A/'image_routing.json').read_text(encoding='utf8'))
    catalog=json.loads((A/'image_ids.sample.json').read_text(encoding='utf8'))
    events=json.loads((A/'events.sample.json').read_text(encoding='utf8'))
    byevent={e['eventId']:e for e in events}
    entries=[]; thumbs=[]
    originals=ROOT/'output/week10/originals';originals.mkdir(parents=True,exist_ok=True)
    for src in sources:
        key=src['id']; source=Path(src['source'])
        im=Image.open(source).convert('RGB')
        shutil.copy2(source,originals/(key+'.png'))
        # Preserve whole composition; fit rather than crop if the provider changed aspect.
        im.thumbnail((300,225),Image.Resampling.NEAREST)
        app=Image.new('RGB',(300,225),im.getpixel((0,0)))
        app.paste(im,((300-im.width)//2,(225-im.height)//2))
        imageid=byevent[key]['imageId'] if key in byevent else 'img_week10_'+key
        path='week10/scenes/'+imageid+'.png'
        dest=A/'images'/path;dest.parent.mkdir(parents=True,exist_ok=True);app.save(dest)
        usage='pov' if key.startswith('path_') else 'scene'
        item={'imageId':imageid,'assetPath':path,'fileName':dest.name,'usage':usage,'period':None,'statTag':None,'description':src['prompt'],'fallbackText':'장면을 준비 중입니다'}
        routing['assets']=[x for x in routing['assets'] if x['imageId']!=imageid]+[item]
        c=next((x for x in catalog if x['imageId']==imageid),None)
        if c is None:c={'imageId':imageid,'stage':'공용'};catalog.append(c)
        c.update({k:item[k] for k in ['usage','fileName','description','fallbackText']});c.update(width=300,height=225)
        if key.startswith('bg_'):routing.setdefault('periodScenes',{})[key[3:].upper()]=imageid
        if key.startswith('ending_'):routing.setdefault('endingScenes',{})[key[7:]]=imageid
        entries.append({**item,'key':key,'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'sourceSize':Image.open(source).size})
        thumb=Image.new('RGB',(300,245),'#f3ecdd');thumb.paste(app,(0,0));ImageDraw.Draw(thumb).text((5,228),key,fill='#302d37');thumbs.append(thumb)
    for file,obj in [(A/'image_routing.json',routing),(A/'image_ids.sample.json',catalog),(DOC/'week10_scene_manifest.json',entries)]:file.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    out=ROOT/'output/week10';out.mkdir(parents=True,exist_ok=True)
    sheet=Image.new('RGB',(1200,245*((len(thumbs)+3)//4)),'#f3ecdd')
    for i,im in enumerate(thumbs):sheet.paste(im,((i%4)*300,(i//4)*245))
    sheet.save(out/'scene_contact_sheet.png')
    print('Packaged',len(entries),'unique scenes',len(set(x['sha256'] for x in entries)))
if __name__=='__main__':main()
