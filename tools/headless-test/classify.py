import sys,re
S=sys.argv[1]; log=sys.argv[2]
mods=set(open(S+'/newclasses.txt').read().split())
txt=open(log).read()
txt=txt[txt.index('=== PROBLEMS'):]
entries=re.split(r'\n(?=\d+x )',txt)
UI=('com.watabou.','com.badlogic.','.scenes.','.ui.','.windows.','.effects.','.sprites.','.tiles.','.utils.GLog')
seen={}
for e in entries:
    if not re.match(r'\d+x (ACT|LEVELGEN)',e) or 'STUCK' in e: continue
    lines=e.split('\n'); n=int(lines[0].split('x')[0])
    frames=[l.strip()[3:] for l in lines[1:] if l.strip().startswith('at ')]
    head=lines[0][lines[0].find(' ')+1:]
    if not frames: continue
    # first frame inside game logic
    top=frames[0]
    if any(u in top for u in ('.ui.','.windows.','Toast','RenderedTextBlock')) or 'Game.platform' in head: continue
    if any(u in top for u in UI) and ('getBackBufferWidth' in head or 'font' in head or 'uiCamera' in head or 'curEmitter' in head or 'Gdx.' in head or 'GameScene.scene' in head or 'cellSelector' in head):
        continue
    if 'sprite" is null' in head or 'sprite.parent' in head or '"this.parent" is null' in head or '"group" is null' in head: continue
    key=head[:200]+'|'+top
    if key in seen: seen[key][0]+=n; continue
    hit=[m for m in mods if re.search(r'\.'+m+r'[\.\$:(]',e)]
    seen[key]=[n,hit,head[:300],frames[:8]]
for k,(n,hit,head,frames) in sorted(seen.items(), key=lambda x: not x[1][1]):
    print(f"{n}x {'MOD'+str(hit) if hit else 'base'} :: {head}")
    for f in frames: print('    '+f)
