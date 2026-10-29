"""Render QA comparisons from the final independent PNG layers, without generating artwork."""
from package_natural_avatars import *
def options(p):
    sheet=Image.new('RGB',(1152,2040),'#ffebca');d=ImageDraw.Draw(sheet)
    for i,(s,h,e) in enumerate(product(SKINS,HAIRS,EYES)):
        im=compose(p,'standard',s,h,e);x=(i%6)*192;y=(i//6)*340;sheet.paste(im,(x,y),im)
        d.text((x+3,y+315),s.replace('skin_','')+'/'+h.replace('hair_',''),fill='#332d37')
        d.text((x+3,y+327),e,fill='#332d37')
    sheet.save(OUT/f'options_36_{p}.png')
if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--pilot',action='store_true');args=parser.parse_args()
    for p in ['young_adult'] if args.pilot else PERIODS:options(p)
    parts=['body/body_young_adult_standard.png','face/skin_young_adult_skin_default_standard.png','face/eyes_young_adult_eyes_default.png','face/hair_young_adult_hair_default.png']
    sheet=Image.new('RGB',(960,340),'#ffebca');d=ImageDraw.Draw(sheet)
    for i,file in enumerate(parts):
        im=Image.open(A/file);sheet.paste(im,(i*192,0),im);d.text((i*192+8,316),['BODY','SKIN + FACE','EYES','HAIR'][i],fill='#332d37')
    im=compose('young_adult','standard');sheet.paste(im,(768,0),im);d.text((776,316),'COMBINED',fill='#332d37')
    sheet.save(OUT/'independent_parts.png')
