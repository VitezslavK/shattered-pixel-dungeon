"""City: Golem Archmage (alternative boss), Light Eater, Mirror Beast, Treasure Goblin."""
from pix import Canvas, hexcol, mix
from mobkit import frames, fade, flash, squash, replace_rows

ARCH_PAL = {
	'k': hexcol('#141620'), 'd': hexcol('#3a3e4a'), 'g': hexcol('#5c6270'), 'G': hexcol('#868c9a'),
	'h': hexcol('#b4b9c4'), 'c': hexcol('#6af3ff'), 'C': hexcol('#2a8ad0'), 'w': hexcol('#e8ffff'),
	'n': hexcol('#5c3b1d'), 'N': hexcol('#8a5a2c'),
}

# Golem Archmage, 20x22: a stone construct in carved robes, rune eyes, a crystal staff
ARCHMAGE = [
	"................kk..",
	"......kkkkk....kcwk.",
	".....kGhhGGk...kCck.",
	"....kGhGGGGgk...kk..",
	"....kGkccGkck...kN..",
	"....kGGGGGGgk...kN..",
	".....kgGGGgk....kN..",
	"....kkkdddkkk...kN..",
	"...kGhkgcgkGGk.kN...",
	"..kGhGkgcgkGGGkkN...",
	"..kGGkggcggkGgkGGk..",
	"..kgk.kgcgk.kgkGhk..",
	"..kkk.kgggk.kkkkk...",
	"......kgcgk.........",
	".....kggcggk........",
	".....kgggggk........",
	"....kggcgcggk.......",
	"....kdgggggdk.......",
	"....kkkkkkkkk.......",
]


def archmage_frame(dy=0, glow=0.0, staff_up=False, step=0):
	c = Canvas(20, 22)
	rows = ARCHMAGE
	if staff_up:
		rows = replace_rows(rows, {0: "..............kk....", 1: "......kkkkk..kcwk...", 2: ".....kGhhGGk.kCck...",
		                            3: "....kGhGGGGgk.kk....", 4: "....kGkccGkck.kN....", 5: "....kGGGGGGgk.kN....",
		                            6: ".....kgGGGgk..kN....", 7: "....kkkdddkkkkN.....", 8: "...kGhkgcgkGGkN.....",
		                            9: "..kGhGkgcgkGGGk.....", 10: "..kGGkggcggkGgk.....", 11: "..kgk.kgcgk.kgk....."})
	c.grid(rows, ARCH_PAL, 0, 2 + dy)
	if glow:
		c = c.recolor(lambda p: mix(p, hexcol('#9af8ff'), glow) if p in (ARCH_PAL['c'], ARCH_PAL['C']) else p)
		for x, y in ((17, 1 + dy), (19, 3 + dy), (15, 2 + dy)):
			c.set(x, y, ARCH_PAL['w'])
	return c


def golem_archmage():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return archmage_frame(dy=[0, 0, 1, 1][s], glow=[0, 0.3, 0.5, 0.3][s])
		if m == 'run':
			return archmage_frame(dy=[0, 1, 0, 1][s])
		if m in ('attack', 'zap'):
			return [archmage_frame(staff_up=True, glow=0.4), archmage_frame(staff_up=True, glow=0.9), archmage_frame(glow=0.3)][s]
		base = archmage_frame()
		return [flash(base, hexcol('#9af8ff'), 0.4), fade(squash(base, 0.85), 0.3), fade(squash(base, 0.6), 0.6), fade(squash(base, 0.4), 0.9)][s]
	return frames(draw)


EATER_PAL = {
	'k': hexcol('#050308'), 'd': hexcol('#140c1e'), 'f': hexcol('#22163a'), 'F': hexcol('#3a2862'),
	'p': hexcol('#7a50c0'), 'w': hexcol('#f4f0ff'), 'r': hexcol('#3a0a20'), 'e': hexcol('#ffffff'),
}

# Light Eater, 16x16: a floating blot of darkness that is mostly mouth
EATER = [
	".....kkkkk......",
	"...kkFFpFFkk....",
	"..kFFpFFFFFFk...",
	".kFFFFFeFFeFk...",
	".kFFFFFFFFFFFk..",
	"kFFFkwkwkwkwFk..",
	"kFFkrrrrrrrrkk..",
	"kfFkrrrrrrrrrk..",
	"kfFFkwkwkwkwkk..",
	".kffFFFFFFFFk...",
	".kdffffffffk....",
	"..kd.kd.kd.k....",
	"..k..kd..k......",
	"......k.........",
]
EATER_CLOSED = {5: "kFFFFFFFFFFFFk..", 6: "kFFkkwkwkwkwkk..", 7: "kfFFkkkkkkkkFk..", 8: "kfFFFFFFFFFFFk.."}


def eater_frame(dy=0, closed=False, shine=0, wide=False):
	c = Canvas(16, 16)
	rows = replace_rows(EATER, EATER_CLOSED) if closed else EATER
	if wide:
		rows = replace_rows(rows, {4: "kFFFkwkwkwkwFk..", 5: "kFFkrrrrrrrrrk..", 6: "kFkrrrrrrrrrrrk.", 7: "kfkrrrrrrrrrrrk.", 8: "kfFkrrrrrrrrrk..", 9: ".kffkwkwkwkwk..."})
	c.grid(rows, EATER_PAL, 1, 1 + dy)
	if shine:
		c.set(4 + shine, 2 + dy, EATER_PAL['p'])
	return c


def light_eater():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [eater_frame(closed=True), eater_frame(closed=True, dy=1, shine=1), eater_frame(dy=1), eater_frame(shine=2)][s]
		if m == 'run':
			return eater_frame(dy=[0, 1, 1, 0][s], closed=s % 2 == 0)
		if m in ('attack', 'zap'):
			return [eater_frame(wide=True), eater_frame(wide=True, dy=-1), eater_frame(closed=True)][s]
		base = eater_frame()
		return [flash(base, hexcol('#ffffff'), 0.5), fade(base, 0.4), fade(squash(base, 0.6), 0.7), fade(squash(base, 0.3), 0.95)][s]
	return frames(draw)


MIRROR_PAL = {
	'k': hexcol('#1a2230'), 'd': hexcol('#5a6a80'), 'f': hexcol('#8a9ab0'), 'F': hexcol('#b8c6d8'),
	'w': hexcol('#f4fbff'), 'c': hexcol('#7ad8f0'), 'C': hexcol('#3a90c0'), 'e': hexcol('#ff5ad0'),
}

# Mirror Beast, 18x14: a lynx made of mirror shards, each facet catching a different light
MIRROR = [
	"...........kk.kk..",
	"..........kFkkFk..",
	"..k.......kFFFFFk.",
	".kck.....kFwFeFFFk",
	".kcCk...kFFFFFFFFk",
	"..kCkkkkkdFcFFFwk.",
	"..kwFcFwFdkFFkkk..",
	".kFcwFFcFFdkk.....",
	".kFFFcFFwFFk......",
	"..kdFFFcFFdk......",
]
MIRROR_LEGS = {
	'stand': ["..kdk..kdk........", "..kdkk.kdkk.......", "..kfkf.kfkf.......", "..kkkk.kkkk......."],
	'run1':  [".kdk....kdk.......", "kdk.kk.kdk.kk.....", "kk...kkkk...kk....", ".................."],
	'run2':  ["...kdkkdk.........", "...kdkkdk.........", "...kfkkfk.........", "...kkkkkk........."],
}


def mirror_frame(legs='stand', dx=0, dy=0, glint=0):
	c = Canvas(18, 14)
	c.grid(MIRROR_LEGS[legs], MIRROR_PAL, 0, 10 + dy)
	c.grid(MIRROR, MIRROR_PAL, dx, dy)
	# a glint that wanders over the facets
	spots = [(4, 7), (8, 6), (6, 8), (11, 4)]
	x, y = spots[glint % 4]
	c.set(x + dx, y + dy, MIRROR_PAL['w'])
	return c


def mirror_beast():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return mirror_frame(glint=s)
		if m == 'run':
			return [mirror_frame('run1', dy=-1, glint=0), mirror_frame('run2', glint=1), mirror_frame('run1', dy=-1, glint=2), mirror_frame('run2', glint=3)][s]
		if m in ('attack', 'zap'):
			return [mirror_frame(dx=-1), mirror_frame('run1', dx=1, dy=-1, glint=3), mirror_frame(dx=1)][s]
		base = mirror_frame()
		return [flash(base, hexcol('#ffffff'), 0.6), fade(base, 0.4), fade(squash(base, 0.6), 0.7), fade(squash(base, 0.3), 0.95)][s]
	return frames(draw)


GOBLIN_PAL = {
	'k': hexcol('#121008'), 'g': hexcol('#4a8a3a'), 'G': hexcol('#6ab84e'), 'h': hexcol('#9ad870'),
	'e': hexcol('#ffe040'), 'b': hexcol('#7a5a34'), 'B': hexcol('#a8804a'), 'y': hexcol('#ffd040'),
	'Y': hexcol('#c08a20'), 'r': hexcol('#a03030'), 'w': hexcol('#fff8d8'),
}

# Treasure Goblin, 14x15: a little goblin hauling a sack bigger than himself, coins spilling out
GOBLIN = [
	"...kkk........",
	"..kBBBk.......",
	".kBByBBk......",
	".kBBBBBkk.....",
	"kBByBBBBGk....",
	"kBBBBBkGhGk...",
	"kBByBBkGkekk..",
	"kBBBBBkhGGGGk.",
	".kBBBkGGwGGk..",
	"..kkk.krrrk...",
	"......krrrGk..",
	"......kbkbk...",
]
GOBLIN_LEGS = {
	'stand': ["......kgkgk...", "......kkkkk...", ".............."],
	'run1':  [".....kgk.kgk..", ".....kk...kk..", ".............."],
	'run2':  ["......kgkgk...", ".......kkk....", ".............."],
}


def goblin_frame(legs='stand', dy=0, coin=None, toss=False):
	c = Canvas(14, 15)
	c.grid(GOBLIN_LEGS[legs], GOBLIN_PAL, 0, 12 + dy)
	rows = GOBLIN
	if toss:
		rows = replace_rows(rows, {9: "..kkk.krrrkGk.", 10: "......krrrkyk.", 11: "......kbkbk..."})
	c.grid(rows, GOBLIN_PAL, 0, dy)
	if coin:
		c.set(coin[0], coin[1], GOBLIN_PAL['y'])
	return c


def treasure_goblin():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [goblin_frame(), goblin_frame(coin=(1, 13)), goblin_frame(dy=1), goblin_frame()][s]
		if m == 'run':
			return [goblin_frame('run1', dy=-1, coin=(0, 9)), goblin_frame('run2'), goblin_frame('run1', dy=-1), goblin_frame('run2', coin=(1, 12))][s]
		if m in ('attack', 'zap'):
			return [goblin_frame(toss=True), goblin_frame(toss=True, coin=(13, 7)), goblin_frame()][s]
		base = goblin_frame()
		return [flash(base, hexcol('#ffd040'), 0.4), fade(squash(base, 0.8), 0.3), fade(squash(base, 0.55), 0.6), fade(squash(base, 0.35), 0.9)][s]
	return frames(draw)
