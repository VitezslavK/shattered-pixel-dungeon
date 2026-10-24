"""Regenerates all of the mod's own sprite files from the designs in this folder.
Run: ~/.spd-build/artenv/bin/python tools/art/build_all.py"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
import heroes

ASSETS = os.path.join(os.path.dirname(__file__), '..', '..', 'core', 'src', 'main', 'assets')
SPRITES = os.path.join(ASSETS, 'sprites')

for cls in (heroes.Necromancer, heroes.Arcanist, heroes.Alchemist, heroes.Smith):
	heroes.build(cls, os.path.join(SPRITES, cls.name + '_hero.png'))
	print('hero', cls.name)

# ending-scene portraits, 24x32 each in hero class order (necromancer is the 7th class)
from PIL import Image
av_path = os.path.join(SPRITES, 'avatars.png')
av = Image.open(av_path).convert('RGBA')
for i, cls in enumerate((heroes.Necromancer, heroes.Arcanist, heroes.Alchemist, heroes.Smith)):
	x = (6 + i) * 24
	av.paste(Image.new('RGBA', (24, 32), (0, 0, 0, 0)), (x, 0))
	av.paste(heroes.avatar(cls).image(), (x, 0))
av.save(av_path)
print('avatars')

# hero select splashes
import splash
for n in ('necromancer', 'arcanist', 'alchemist', 'smith'):
	splash.save(getattr(splash, n)(), os.path.join(ASSETS, 'splashes', n + '.jpg'))
print('splashes')

# subclass, ability and evolution icons
import icons
hi_path = os.path.join(ASSETS, 'interfaces', 'hero_icons.png')
hi = Image.open(hi_path).convert('RGBA')
for slot, c in icons.all_icons():
	x, y = (slot % 8) * 16, (slot // 8) * 16
	hi.paste(Image.new('RGBA', (16, 16), (0, 0, 0, 0)), (x, y))
	hi.paste(c.image(), (x, y))
hi.save(hi_path)
print('hero icons')

# talent icons, 32 per row
import talents
ti_path = os.path.join(ASSETS, 'interfaces', 'talent_icons.png')
ti = Image.open(ti_path).convert('RGBA')
for slot, c in talents.all_talents():
	x, y = (slot % 32) * 16, (slot // 32) * 16
	ti.paste(Image.new('RGBA', (16, 16), (0, 0, 0, 0)), (x, y))
	ti.paste(c.image(), (x, y))
ti.save(ti_path)
print('talent icons')

# monsters: one 18-frame sheet each in sprites/mod (layout in sprites/ModMobFrames.java)
import pix
import mobs_sewers
MOD = os.path.join(SPRITES, 'mod')
os.makedirs(MOD, exist_ok=True)
MOBS = {
	'sewer_leech': mobs_sewers.sewer_leech,
	'rat_matriarch': mobs_sewers.rat_matriarch,
	'rat_brood': mobs_sewers.brood,
}
for name, fn in MOBS.items():
	pix.sheet(fn(), os.path.join(MOD, name + '.png'))
print('monsters', len(MOBS))

import mobs_prison, mobs_caves, mobs_city, mobs_deep
MOBS2 = {
	'prison_hound': mobs_prison.prison_hound, 'jailer': mobs_prison.jailer,
	'crystal_crawler': mobs_caves.crystal_crawler, 'crystal_queen': mobs_caves.crystal_queen,
	'brass_golem': mobs_caves.brass_golem,
	'golem_archmage': mobs_city.golem_archmage, 'light_eater': mobs_city.light_eater,
	'mirror_beast': mobs_city.mirror_beast, 'treasure_goblin': mobs_city.treasure_goblin,
	'shade': mobs_deep.shade, 'void_walker': mobs_deep.void_walker, 'void_herald': mobs_deep.void_herald,
	'fallen_angel': mobs_deep.fallen_angel, 'archlich': mobs_deep.archlich,
	'skeletal_servant': mobs_deep.skeletal_servant, 'risen': mobs_deep.risen, 'homunculus': mobs_deep.homunculus,
	'shrine_blood': lambda: mobs_deep.shrine('blood'), 'shrine_gold': lambda: mobs_deep.shrine('gold'),
	'shrine_arcane': lambda: mobs_deep.shrine('arcane'),
	'golden_altar': mobs_deep.golden_altar,
}
for name, fn in MOBS2.items():
	pix.sheet(fn(), os.path.join(MOD, name + '.png'))
# the guardian keeps the Eye's 256x32 layout
g = Image.new('RGBA', (256, 32), (0, 0, 0, 0))
for i, f in enumerate(mobs_deep.amulet_guardian()):
	g.paste(f.image(), (i * 16, 0))
g.save(os.path.join(MOD, 'amulet_guardian.png'))
print('monsters', len(MOBS) + len(MOBS2) + 1)

# item icons: cropped to their content, put at the top left of their cell, sizes written back to Java
import re
import items_mod
ISS = os.path.join(os.path.dirname(__file__), '..', '..', 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                   'shatteredpixeldungeon', 'sprites', 'ItemSpriteSheet.java')
src = open(ISS).read()
vals = {}


def _ev(e):
	e = e.strip()
	m = re.match(r'xy\((\d+),\s*(\d+)\)', e)
	if m:
		return int(m.group(1)) - 1 + (int(m.group(2)) - 1) * 16
	return sum(int(p) if p.strip().isdigit() else vals[p.strip()] for p in e.split('+'))


for m in re.finditer(r'(\w+)\s*=\s*([^;]+);', src):
	try:
		vals[m.group(1)] = _ev(m.group(2))
	except Exception:
		pass
GROUP_SIZE = {'WAND_': (14, 14), 'RING_': (8, 10), 'BREW_': (12, 14)}
it_path = os.path.join(SPRITES, 'items.png')
sheet_im = Image.open(it_path).convert('RGBA')
for name in items_mod.ITEMS:
	im = items_mod.icon(name).image()
	box = im.getbbox()
	im = im.crop(box)
	slot = vals[name]
	x, y = (slot % 16) * 16, (slot // 16) * 16
	sheet_im.paste(Image.new('RGBA', (16, 16), (0, 0, 0, 0)), (x, y))
	group = next((v for k, v in GROUP_SIZE.items() if name.startswith(k)), None)
	if group:
		gw, gh = group
		sheet_im.paste(im, (x + (gw - im.width) // 2, y + gh - im.height))
	else:
		sheet_im.paste(im, (x, y))
		src, n = re.subn(r'assignItemRect\(\s*' + name + r',\s*\d+,\s*\d+\)',
		                 'assignItemRect(%s, %d, %d)' % (name, im.width, im.height), src)
		if n == 0:
			print('no size entry for', name)
sheet_im.save(it_path)
open(ISS, 'w').write(src)
print('items', len(items_mod.ITEMS))

# floor tilesets and water for the mod's regions
import tiles
ENV = os.path.join(ASSETS, 'environment')
for kind, src in (('ice', 'tiles_caves'), ('burned', 'tiles_city'), ('abyss', 'tiles_halls')):
	tiles.retexture(os.path.join(ENV, src + '.png'), kind, os.path.join(ENV, 'tiles_' + kind + '.png'))
	tiles.water(kind, os.path.join(ENV, 'water_' + kind + '.png'))
print('tilesets')
