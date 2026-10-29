"""Audit the independent PNG layers, 648 combinations, body provenance and story routing."""
from pathlib import Path
from itertools import product
import argparse,json,hashlib
from PIL import Image
from package_natural_avatars import PERIODS,BUILDS,SKINS,HAIRS,EYES,SIZE,FOOT,A,OUT,ROOT,source,package_body,active_source_keys
def digest(im):return hashlib.sha256(im.tobytes()).hexdigest()
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verify-sources',action='store_true',help='Also verify private original images and packaging geometry (requires local source files).')
    parser.add_argument('--output',type=Path,default=OUT/'asset_audit.json')
    args=parser.parse_args()
    meta=json.loads((ROOT/'docs/week10/avatar_v4_sources.json').read_text(encoding='utf8'))
    geo=None
    if args.verify_sources:
        geometry=OUT/'geometry.json'
        if not geometry.is_file():parser.error(f'Source verification requires {geometry}; omit --verify-sources for repository asset checks.')
        missing=[value['source'] for value in meta.values() if not Path(value['source']).is_file()]
        if missing:parser.error(f'Source verification requires {len(missing)} missing original images; first: {missing[0]}')
        geo=json.loads(geometry.read_text(encoding='utf8'))
    keys=active_source_keys();assert len(keys)==31 and all(k in meta for k in keys)
    allfiles=list(A.rglob('*.png'));assert len(allfiles)==115,len(allfiles)
    for f in allfiles:
        im=Image.open(f);assert im.mode=='RGBA',f
        if f.parent.name=='examples':assert im.size==(1152,312);continue
        assert im.size==SIZE,f
        assert set(im.getchannel('A').getdata())=={0,255},f
        box=im.getchannel('A').getbbox();assert box and 0<box[0]<box[2]<192 and 0<=box[1]<box[3]<312,(f,box)
    seen=set();bodies=set();sourcehash=set();colorcount=0
    for p,b,s,h,e in product(PERIODS,BUILDS,SKINS,HAIRS,EYES):
        paths=[f'body/body_{p}_{b}.png',f'face/skin_{p}_{s}_{b}.png',f'face/eyes_{p}_{e}.png',f'face/hair_{p}_{h}.png']
        layers=[Image.open(A/f).convert('RGBA') for f in paths]
        composite=Image.new('RGBA',SIZE)
        for layer in layers:composite.alpha_composite(layer)
        assert composite.getchannel('A').getbbox()[3]==FOOT,(p,b,s,h,e)
        seen.add(digest(composite));bodies.add(digest(layers[0]))
        colorcount=max(colorcount,len({rgba for rgba in composite.getdata() if rgba[3]}))
        # Eye layers must still be visible on both sides after hair composition.
        for lo,hi in [(0,96),(96,192)]:
            visible=sum(1 for y in range(312) for x in range(lo,hi) if layers[2].getpixel((x,y))[3] and not layers[3].getpixel((x,y))[3])
            assert visible>4,(p,h,e,'hidden eyes',visible)
    assert len(seen)==648,len(seen);assert len(bodies)==18,len(bodies)
    for p,b in product(PERIODS,BUILDS):
        key=p+'_'+b
        if args.verify_sources:
            sourcehash.add(hashlib.sha256(Path(meta[key]['source']).read_bytes()).hexdigest())
            raw=source(key,meta);expected=package_body(raw,meta[key]['neckRow'],geo[p]['neckAnchor'][1])
            actual=Image.open(A/'body'/f'body_{p}_{b}.png')
            assert digest(expected)==digest(actual),(p,b,'source mismatch')
        # Both arms/hands must receive the dark skin selection; footwear must retain its colors.
        light=Image.open(A/'face'/f'skin_{p}_skin_default_{b}.png')
        dark=Image.open(A/'face'/f'skin_{p}_skin_deep_{b}.png')
        ny=geo[p]['neckAnchor'][1] if geo else 0
        for lo,hi in [(0,96),(96,192)]:
            changed=sum(1 for y in range(ny+22,FOOT-18) for x in range(lo,hi) if light.getpixel((x,y))!=dark.getpixel((x,y)) and light.getpixel((x,y))[3])
            assert changed>12,(p,b,'untinted hand/arm',changed)
        assert all(not dark.getpixel((x,y))[3] for y in range(FOOT-10,312) for x in range(192)),(p,b,'shoe recoloring')
    if args.verify_sources:assert len(sourcehash)==18
    scenes=json.loads((ROOT/'docs/week10/week10_scene_manifest.json').read_text(encoding='utf8'))
    hashes={hashlib.sha256((ROOT/'app/src/main/assets/images'/x['assetPath']).read_bytes()).hexdigest() for x in scenes}
    assert len(scenes)==len(hashes)==24
    events={x['eventId']:x for x in json.loads((ROOT/'app/src/main/assets/events.sample.json').read_text(encoding='utf8'))}
    for x in scenes:
        if x['key'].startswith('path_'):assert events[x['key']]['imageId']==x['imageId']
    report={'status':'PASS','validationMode':'sources' if args.verify_sources else 'repository-assets','bodySprites':18,'uniqueBodySources':len(sourcehash) if args.verify_sources else None,'sourceBoundBodyChecks':18 if args.verify_sources else 0,'independentLayerKinds':['body','skin','eyes','hair'],
        'skinOptions':3,'hairOptions':4,'eyeOptions':3,'distinctComposites':len(seen),'avatarPngFiles':len(allfiles),
        'packagingSourceFilesIncludingExtractionEdits':31,'maximumVisibleRGBAColors':colorcount,
        'newUniqueScenes':24,'routedDedicatedEvents':14,'faceSheetExample':1,'actualUserPhotoGeneration':False,'errors':[]}
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(report,indent=2),encoding='utf8');print(json.dumps(report))
if __name__=='__main__':main()
