"""Crop, align and palette-map generated independent body/skin/eyes/hair assets."""
from pathlib import Path
from itertools import product
from collections import deque
import argparse,json,hashlib,shutil,colorsys
from PIL import Image,ImageDraw,ImageOps
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'app/src/main/assets/images/avatar_v4';OUT=ROOT/'output/week10/avatar_v4'
META=ROOT/'docs/week10/avatar_v4_sources.json'
PERIODS=['infant','child','teen','young_adult','middle','old_age']
BUILDS=['slim','standard','muscular'];SKINS=['skin_default','skin_tan','skin_deep']
HAIRS=['hair_default','hair_bob_brown','hair_long_black','hair_curly_brown']
EYES=['eyes_default','eyes_big','eyes_narrow']
SIZE=(192,312);FOOT=304
HEIGHTS={'infant':166,'child':214,'teen':274,'young_adult':290,'middle':284,'old_age':274}
SOURCE_CACHE={}
def blank():return Image.new('RGBA',SIZE)
def clean(im):
    im=im.convert('RGBA');im.putalpha(im.getchannel('A').point(lambda v:255 if v>=200 else 0))
    im.putdata([(r,g,b,a) if a else (0,0,0,0) for r,g,b,a in im.getdata()])
    box=im.getchannel('A').getbbox();assert box
    return im.crop(box)
def source(key,meta):
    if key in SOURCE_CACHE:return SOURCE_CACHE[key].copy()
    path=Path(meta[key]['source']);dest=OUT/'originals'/f'{key}.png';dest.parent.mkdir(parents=True,exist_ok=True)
    if path.resolve()!=dest.resolve():shutil.copy2(path,dest)
    meta[key]['sha256']=hashlib.sha256(path.read_bytes()).hexdigest()
    result=clean(Image.open(path));SOURCE_CACHE[key]=result;return result.copy()
def split_row(im):
    alpha=im.getchannel('A');rows=[]
    for y in range(round(im.height*.25),round(im.height*.48)):
        box=alpha.crop((0,y,im.width,y+1)).getbbox()
        if box:rows.append((box[2]-box[0],y))
    minimum=min(w for w,y in rows)
    return min(y for w,y in rows if w<=minimum+2)
def components(mask):
    remaining=set(mask);groups=[]
    while remaining:
        seed=remaining.pop();group={seed};queue=deque([seed])
        while queue:
            x,y=queue.popleft()
            for point in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]:
                if point in remaining:remaining.remove(point);group.add(point);queue.append(point)
        groups.append(group)
    return groups
def bounds(points):return(min(x for x,y in points),min(y for x,y in points),max(x for x,y in points)+1,max(y for x,y in points)+1)
def exposed_skin_mask(im,neckY,period):
    candidate=set();limit=FOOT-(15 if period=='infant' else 18)
    for y in range(limit):
        for x in range(im.width):
            r,g,b,a=im.getpixel((x,y));h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
            if a and 8<=h*360<=35 and .18<s<.72 and r>205:candidate.add((x,y))
    chosen=set();bodyH=FOOT-neckY;bodyBox=im.getchannel('A').getbbox();bodyWidth=bodyBox[2]-bodyBox[0]
    for group in components(candidate):
        l,t,r,b=bounds(group);cx=(l+r)/2
        bright=sum(im.getpixel(p)[0] for p in group)/len(group)>225 and any(im.getpixel(p)[0]>235 for p in group)
        neck=t<=neckY+5 and b<neckY+bodyH*.30 and abs(cx-96)<18
        outer=abs(cx-96)>bodyWidth*.25
        bareLeg=period in ('infant','child') and t>neckY+bodyH*.60
        exposed=(outer or bareLeg) and t>neckY+bodyH*.20 and b<=limit and (b-t)<bodyH*.40
        if bright and (neck or exposed):chosen.update(group)
    return chosen
def tint(im,skin,mask=None):
    if skin=='skin_default':return im.copy()
    factors=(.83,.73,.65) if skin=='skin_tan' else (.56,.45,.38)
    out=im.copy();px=out.load()
    points=mask if mask is not None else [(x,y) for y in range(im.height) for x in range(im.width) if px[x,y][3]]
    for x,y in points:
        r,g,b,a=px[x,y];px[x,y]=(int(r*factors[0]),int(g*factors[1]),int(b*factors[2]),a)
    return out
def save(im,path):path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
def place(im,width,top):
    height=round(im.height*width/im.width);layer=blank()
    layer.alpha_composite(im.resize((width,height),Image.Resampling.NEAREST),(96-width//2,top))
    return layer
def compose(p,b,s='skin_default',h='hair_default',e='eyes_default'):
    im=blank()
    for file in [f'body/body_{p}_{b}.png',f'face/skin_{p}_{s}_{b}.png',f'face/eyes_{p}_{e}.png',f'face/hair_{p}_{h}.png']:
        im.alpha_composite(Image.open(A/file))
    return im
def package_body(raw,row,neckY):
    cropped=raw.crop((0,row,raw.width,raw.height));box=cropped.getchannel('A').getbbox();cropped=cropped.crop(box)
    height=FOOT-neckY;scale=height/cropped.height;width=round(cropped.width*scale)
    assert width<184,width
    neckBox=raw.getchannel('A').crop((0,row,raw.width,row+1)).getbbox();neckX=(neckBox[0]+neckBox[2])/2
    left=96-round((neckX-box[0])*scale)
    body=blank();body.alpha_composite(cropped.resize((width,height),Image.Resampling.NEAREST),(left,neckY))
    return body
def active_source_keys():
    return [p+'_'+b for p,b in product(PERIODS,BUILDS)]+['face_'+p for p in PERIODS]+['hair_extract_'+h for h in HAIRS]+['eye_extract_'+e for e in EYES]
def package(pilot=False):
    meta=json.loads(META.read_text(encoding='utf8'));periods=['young_adult'] if pilot else PERIODS;geometry={};gallery=[]
    for p in periods:
        default=source(p+'_standard',meta);cut=meta[p+'_standard'].get('neckRow',split_row(default))
        scale=HEIGHTS[p]/default.height;neckY=FOOT-HEIGHTS[p]+round(cut*scale)
        rawface=source('face_'+p,meta);row=meta['face_'+p].get('neckRow',rawface.height-(default.height-cut))
        facepart=rawface.crop((0,0,rawface.width,row));box=facepart.getchannel('A').getbbox();facepart=facepart.crop(box)
        faceheight=round(facepart.height*scale);facewidth=round(facepart.width*scale)
        face=place(facepart,facewidth,neckY-faceheight)
        eyeWidth=round(facewidth*.65);eyeTop=neckY-round(facewidth*.58)
        geometry[p]={'headTop':FOOT-HEIGHTS[p],'faceBox':list(face.getchannel('A').getbbox()),'faceCenterX':96,
            'neckAnchor':[96,neckY],'footAnchor':[96,FOOT],'sourceNeckRow':cut,'sourceFaceNeckRow':row,
            'eyesTop':eyeTop,'eyesWidth':eyeWidth,'hair':{}}
        meta['face_'+p]['neckRow']=row
        for b in BUILDS:
            key=p+'_'+b;raw=source(key,meta);row=meta[key].get('neckRow',split_row(raw));body=package_body(raw,row,neckY)
            save(body,A/'body'/f'body_{p}_{b}.png');meta[key]['neckRow']=row
            mask=exposed_skin_mask(body,neckY,p)
            for s in SKINS:
                overlay=blank();tinted=tint(body,s,mask)
                for point in mask:overlay.putpixel(point,tinted.getpixel(point))
                overlay.alpha_composite(tint(face,s));save(overlay,A/'face'/f'skin_{p}_{s}_{b}.png')
        for e in EYES:
            key='eye_extract_'+e
            if key in meta:save(place(source(key,meta),eyeWidth,eyeTop),A/'face'/f'eyes_{p}_{e}.png')
        for h,factor,dy in [('hair_default',1.25,0),('hair_bob_brown',1.23,2),('hair_long_black',1.25,2),('hair_curly_brown',1.26,0)]:
            key='hair_extract_'+h
            if key not in meta:continue
            raw=source(key,meta);width=round(facewidth*factor);top=FOOT-HEIGHTS[p]+dy
            if p=='old_age':
                alpha=raw.getchannel('A');raw=ImageOps.grayscale(raw).point(lambda v:min(238,145+round(v*.65))).convert('RGBA');raw.putalpha(alpha)
            layer=place(raw,width,top);save(layer,A/'face'/f'hair_{p}_{h}.png')
            geometry[p]['hair'][h]={'top':top,'width':width,'bounds':list(layer.getchannel('A').getbbox())}
        for b in BUILDS:gallery.append((p,b,compose(p,b)))
    OUT.mkdir(parents=True,exist_ok=True)
    sheet=Image.new('RGB',(576,340*len(periods)),'#ffebca');d=ImageDraw.Draw(sheet)
    for i,(p,b,im) in enumerate(gallery):
        x=(i%3)*192;y=(i//3)*340;sheet.paste(im,(x,y),im);d.text((x+8,y+315),p+'/'+b,fill='#332d37')
    sheet.save(OUT/('pilot_separate_packaged.png' if pilot else 'all_18_packaged.png'))
    if not pilot:
        sheet=Image.new('RGBA',(1152,312))
        for i,p in enumerate(PERIODS):
            head=blank()
            for name in [f'skin_{p}_skin_default_standard.png',f'eyes_{p}_eyes_default.png',f'hair_{p}_hair_default.png']:
                im=Image.open(A/'face'/name).copy()
                if name.startswith('skin_'):im=im.crop((0,0,192,geometry[p]['neckAnchor'][1]))
                head.alpha_composite(im)
            sheet.alpha_composite(head,(i*192,0))
        save(sheet,A/'examples/face_sheet_example.png')
    META.write_text(json.dumps(meta,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    (OUT/'geometry.json').write_text(json.dumps(geometry,indent=2),encoding='utf8')
    print('Packaged independent body/skin/eyes/hair layers; body count:',len(gallery))
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--pilot',action='store_true');args=parser.parse_args();package(args.pilot)
