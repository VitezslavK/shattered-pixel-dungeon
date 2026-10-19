"""Halls and the abyss: Shade, Void Walker, Void Herald, Fallen Angel, Archlich, Amulet Guardian.
Allies and props: skeletal servant, risen, homunculus, shrines."""
import math
from pix import Canvas, hexcol, mix
from mobkit import frames, fade, flash, squash, replace_rows


def death(base, col='#ffffff'):
	return [flash(base, hexcol(col), 0.45), fade(base, 0.35), fade(squash(base, 0.6), 0.65), fade(squash(base, 0.3), 0.95)]


# ------------------------------------------------------------------ Shade 14x16
SHADE_PAL = {'k': hexcol('#06040c'), 'd': hexcol('#140e24'), 'f': hexcol('#22183a'), 'F': hexcol('#32244e'),
             'e': hexcol('#d08aff'), 'E': hexcol('#ffffff')}
SHADE = [
	"....kkkkk.....",
	"...kFFFFFk....",
	"..kFFdddFFk...",
	"..kFdkkkdFk...",
	"..kFdkEkekk...",
	"..kFdkkkkFk...",
	"..kFFdddFFFk..",
	".kFFFFFFFFFFk.",
	"kFfkFFFFFFkfFk",
	"kfk.kFFFFFk.kk",
	"kk..kfFFFfk...",
	"....kffFffk...",
	"....kf.kf.k...",
	"...kf..kf.....",
	"...k....k.....",
]


def shade_frame(dy=0, sway=0, reach=False):
	c = Canvas(14, 16)
	rows = SHADE
	if sway:
		rows = replace_rows(rows, {12: "....k.fk.fk...", 13: "....kf..kf....", 14: ".....k...k...."})
	if reach:
		rows = replace_rows(rows, {7: ".kFFFFFFFFFFFk", 8: "kFfkFFFFFFFkfk", 9: "kfk.kFFFFFk..k"})
	c.grid(rows, SHADE_PAL, 0, dy)
	return c


def shade():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return shade_frame(dy=[0, 0, 1, 1][s], sway=s % 2)
		if m == 'run':
			return shade_frame(dy=[0, 1, 0, 1][s], sway=(s + 1) % 2)
		if m in ('attack', 'zap'):
			return [shade_frame(reach=True), shade_frame(reach=True, dy=-1), shade_frame()][s]
		return death(shade_frame(), '#d08aff')[s]
	return frames(draw)


# ------------------------------------------------------------------ Void Walker 12x17
VOID_PAL = {'k': hexcol('#030208'), 'd': hexcol('#0e0820'), 'f': hexcol('#1c1238'), 'F': hexcol('#2c1e56'),
            's': hexcol('#b8a8ff'), 'e': hexcol('#7af0ff')}
WALKER = [
	"...kkkk.....",
	"..kFFFFk....",
	"..kFkekk....",
	"..kFFFFk....",
	"...kFFk.....",
	"..kFsFFFk...",
	".kFFFFFsFk..",
	".kfkFsFFkfk.",
	".kfkFFFFkfk.",
	".kfkFFsFkfk.",
	"..k.kFFk.fk.",
	"....kFkFk...",
	"....kfkfk...",
	"....kfkfk...",
	"...kfk.kfk..",
	"...kk...kk..",
]


def walker_frame(dx=0, dy=0, twinkle=0, legs=0, reach=False):
	c = Canvas(12, 17)
	rows = WALKER
	if legs == 1:
		rows = replace_rows(rows, {13: "...kfk.kfk..", 14: "..kfk...kfk.", 15: "..kk.....kk."})
	if reach:
		rows = replace_rows(rows, {6: ".kFFFFFsFFkk", 7: ".kfkFsFFFkfk", 8: ".kfkFFFFk.kk"})
	c.grid(rows, VOID_PAL, dx, dy + 1)
	stars = [(5, 7), (7, 8), (6, 10), (4, 9)]
	x, y = stars[twinkle % 4]
	c.set(x + dx, y + dy, hexcol('#ffffff'))
	return c


def void_walker():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return walker_frame(twinkle=s)
		if m == 'run':
			return walker_frame(legs=s % 2, dy=-(s % 2), twinkle=s)
		if m in ('attack', 'zap'):
			return [walker_frame(reach=True), walker_frame(reach=True, dx=1, twinkle=2), walker_frame()][s]
		return death(walker_frame(), '#7af0ff')[s]
	return frames(draw)


# ------------------------------------------------------------------ Void Herald 22x24
HERALD_PAL = {'k': hexcol('#040208'), 'd': hexcol('#120a24'), 'f': hexcol('#22143e'), 'F': hexcol('#38245e'),
              'h': hexcol('#5a3c8a'), 'b': hexcol('#e8e0d0'), 'B': hexcol('#a89c88'), 'e': hexcol('#ff4af0'),
              's': hexcol('#ffffff'), 'v': hexcol('#9a5aff')}
HERALD = [
	"...b............b.....",
	"...bk..........kb.....",
	"....bk.kkkkkk.kb......",
	"....kbkBbbbbBkbk......",
	".....kBkkbbkkBk.......",
	".....kBkebbekBk.......",
	".....kBbbkkbbBk.......",
	"......kBbbbbBk........",
	".....kkkhhhhkkk.......",
	"....kFFhFFFFhFFk......",
	"...kFFFFFsFFFFFFk.....",
	"..kFFkFFFFFFsFkFFk....",
	"..kfFkFFsFFFFFkFfk....",
	"..kffkFFFFFsFFkffk....",
	"..kkk.kFFFFFFFk.kk....",
	"......kFFsFFFFk.......",
	".....kFFFFFFsFFk......",
	".....kFFFFFFFFFk......",
	"....kfFFsFFFFFFfk.....",
	"....kffffffffffk......",
	"....kkkkkkkkkkkk......",
]


def herald_frame(dy=0, ring=0.0, cast=0.0, arms_up=False):
	c = Canvas(22, 24)
	# a ring of nothing behind the head
	for a in range(0, 360, 12):
		r = math.radians(a + ring * 30)
		x, y = 9.5 + 7 * math.cos(r), 6 + dy + 6 * math.sin(r)
		c.set(x, y, HERALD_PAL['v'] if (a // 12) % 3 else HERALD_PAL['s'])
	rows = HERALD
	if arms_up:
		rows = replace_rows(rows, {9: "..kkFFhFFFFhFFkk......", 10: ".kFkFFFFsFFFFFkFk.....", 11: ".kFkFFFFFFsFFFkFk.....",
		                           12: "..kkFFsFFFFFFFkk......", 13: "...kFFFFFFsFFFk.......", 14: "....kFFFFFFFFk........"})
	c.grid(rows, HERALD_PAL, 0, 2 + dy)
	if cast:
		for x, y in ((2, 9), (17, 9), (9, 1)):
			c.set(x, y + dy, mix(HERALD_PAL['e'], hexcol('#ffffff'), cast))
	return c


def void_herald():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return herald_frame(dy=[0, 0, 1, 1][s], ring=s)
		if m == 'run':
			return herald_frame(dy=[0, 1, 0, 1][s], ring=s)
		if m in ('attack', 'zap'):
			return [herald_frame(arms_up=True, cast=0.3), herald_frame(arms_up=True, cast=1.0, ring=2), herald_frame(ring=3)][s]
		return death(herald_frame(), '#ff4af0')[s]
	return frames(draw)


# ------------------------------------------------------------------ Fallen Angel 18x17
ANGEL_PAL = {'k': hexcol('#141218'), 'w': hexcol('#3a3440'), 'W': hexcol('#5c5466'), 'r': hexcol('#e8e2d4'),
             'R': hexcol('#b0a894'), 's': hexcol('#f2d4b8'), 'S': hexcol('#c4a284'), 'y': hexcol('#ffd76a'),
             'Y': hexcol('#a8822a'), 'h': hexcol('#e8e0c8'), 'e': hexcol('#3a1010'), 'x': hexcol('#c03030')}
ANGEL = [
	".......y.yy.......",
	"......Y....y......",
	".......kkkk.......",
	"..kk..khhhhk...kk.",
	".kWwk.khsesk..kwWk",
	"kWwwk.kssssk.kwwWk",
	"kWwWwk.kssk.kwWwWk",
	".kWwwkkrrrrkkwwWk.",
	"..kWkkrRrrRrkkWk..",
	"...k.krrxrrRk.k...",
	".....krrrrrRk.....",
	".....krrRrrRk.....",
	"....krrrrrrrRk....",
	"....kRrrRrrRRk....",
	".....kkkkkkkk.....",
]


def angel_frame(dy=0, flap=0, strike=False):
	c = Canvas(18, 17)
	rows = ANGEL
	if flap == 1:
		rows = replace_rows(rows, {3: "......khhhhk......", 4: "kk....khsesk....kk", 5: "kWwk..kssssk..kwWk",
		                           6: "kWwwk..kssk..kwwWk", 7: ".kWwwkkrrrrkkwwWk."})
	if strike:
		rows = replace_rows(rows, {8: "..kWkkrRrrRrksssk.", 9: "...k.krrxrrRk.kk.."})
	c.grid(rows, ANGEL_PAL, 0, 1 + dy)
	return c


def fallen_angel():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return angel_frame(dy=[0, 1, 1, 0][s], flap=s // 2)
		if m == 'run':
			return angel_frame(dy=[0, 1, 0, 1][s], flap=s % 2)
		if m in ('attack', 'zap'):
			return [angel_frame(flap=1), angel_frame(strike=True), angel_frame()][s]
		return death(angel_frame(), '#ffd76a')[s]
	return frames(draw)


# ------------------------------------------------------------------ Archlich 18x22
LICH_PAL = {'k': hexcol('#100c14'), 'b': hexcol('#ece5cf'), 'B': hexcol('#b3a98e'), 'g': hexcol('#5dff9a'),
            'y': hexcol('#ffd76a'), 'Y': hexcol('#a8822a'), 'p': hexcol('#4a2a6a'), 'P': hexcol('#2e1a44'),
            'q': hexcol('#6a3c94'), 'n': hexcol('#5c3b1d'), 'r': hexcol('#ff3a5a')}
LICH = [
	"....y.y.y.........",
	"....yyyyy.....kk..",
	"...kkkkkkk...kbbk.",
	"...kbbbbbk...kgbk.",
	"...kbkgkgk...kbbk.",
	"...kbbbkbk....kn..",
	"....kbkbk.....kn..",
	"...kqqkkqqk...kn..",
	"..kqppqqppqk..kn..",
	".kqpkpprppkpk.kn..",
	".kpPkppppPkpkkbn..",
	".kpk.kppPpk.kBBk..",
	".kbk.kpPppk..kn...",
	".kk..kpppPk..kn...",
	".....kppPpPk.kn...",
	"....kppppPPk..k...",
	"....kpPppPPPk.....",
	"....kPPPPPPPk.....",
	"....kkkkkkkkk.....",
]


def lich_frame(dy=0, glow=0.0, raise_staff=False):
	c = Canvas(18, 22)
	rows = LICH
	if raise_staff:
		rows = replace_rows(rows, {0: "....y.y.y....kk..", 1: "....yyyyy...kbbk.", 2: "...kkkkkkk..kgbk.",
		                           3: "...kbbbbbk..kbbk.", 4: "...kbkgkgk...kn..", 5: "...kbbbkbk...kn..",
		                           6: "....kbkbk...kbn..", 7: "...kqqkkqqkkBBk..", 8: "..kqppqqppqkkn...",
		                           9: ".kqpkpprppkpkn....", 10: ".kpPkppppPkpkk....", 11: ".kpk.kppPpk.k....."})
	c.grid(rows, LICH_PAL, 0, 2 + dy)
	if glow:
		c = c.recolor(lambda p: mix(p, hexcol('#ffffff'), glow) if p == LICH_PAL['g'] else p)
	return c


def archlich():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return lich_frame(dy=[0, 0, 1, 1][s], glow=[0, 0.4, 0.6, 0.3][s])
		if m == 'run':
			return lich_frame(dy=[0, 1, 0, 1][s])
		if m in ('attack', 'zap'):
			return [lich_frame(raise_staff=True, glow=0.4), lich_frame(raise_staff=True, glow=1.0), lich_frame()][s]
		return death(lich_frame(), '#5dff9a')[s]
	return frames(draw)


# ------------------------------------------------------------------ Amulet Guardian
# Uses the Eye's own layout (16x18 frames on a 256x32 sheet) so its hidden charging animation lines up:
# 0-2 idle, 3-4 charging, 5-6 run, 7-9 die (attack plays 4,3)
GUARD_PAL = {'k': hexcol('#1a1006'), 'y': hexcol('#ffd76a'), 'Y': hexcol('#c8962e'), 'd': hexcol('#7a5418'),
             'w': hexcol('#ffffff'), 'W': hexcol('#e8e0d0'), 'r': hexcol('#e83a3a'), 'R': hexcol('#8a1a1a'),
             'g': hexcol('#3ad8ff'), 'p': hexcol('#ff7af0'), 'P': hexcol('#9a2a8a')}
GUARD = [
	".......kk.......",
	"......kppk......",
	".....kpPPpk.....",
	"......kPPk......",
	"...kkkkyykkkk...",
	"..kyYYyyyyYYyk..",
	".kyYkkkkkkkkYyk.",
	"kyYkWWWWWWWWkYyk",
	"kyYkWWkrrkWWkYyk",
	"kyYkWkrRRrkWkYyk",
	"kyYkWkrRRrkWkYyk",
	"kyYkWWkrrkWWkYyk",
	".kyYkkkkkkkkYyk.",
	"..kyYYyyyyYYyk..",
	"...kkkdyydkkk...",
	".....kd..dk.....",
	"....kd....dk....",
	"....k......k....",
]


def guardian_frame(dy=0, look=0, charge=0.0, blink=False):
	c = Canvas(16, 18)
	rows = GUARD
	if blink:
		rows = replace_rows(rows, {8: "kyYkkkkkkkkkkYyk", 9: "kyYkYYYYYYYYkYyk", 10: "kyYkYYYYYYYYkYyk", 11: "kyYkkkkkkkkkkYyk"})
	c.grid(rows, GUARD_PAL, 0, dy)
	if look and not blink:
		# pupil shifts
		c = c.shifted(0, 0)
	if charge:
		c = c.recolor(lambda p: mix(p, hexcol('#ffffff'), charge) if p in (GUARD_PAL['r'], GUARD_PAL['R']) else p)
		for x, y in ((1, 3), (14, 3), (0, 15), (15, 15)):
			c.set(x, y, mix(GUARD_PAL['g'], hexcol('#ffffff'), charge))
	return c


def amulet_guardian():
	"""returns the 10 Eye-layout frames"""
	base = guardian_frame()
	return [guardian_frame(), guardian_frame(dy=1), guardian_frame(blink=True),
	        guardian_frame(charge=0.5), guardian_frame(charge=1.0),
	        guardian_frame(dy=1), guardian_frame(),
	        flash(base, hexcol('#ffffff'), 0.5), fade(squash(base, 0.6), 0.6), fade(squash(base, 0.3), 0.95)]


# ------------------------------------------------------------------ Skeletal servant & risen 12x15
SERVANT_PAL = {'k': hexcol('#121010'), 'b': hexcol('#ece5cf'), 'B': hexcol('#a89e84'), 'g': hexcol('#5dff9a'),
               's': hexcol('#8a8f9a'), 'S': hexcol('#5a5e68'), 'n': hexcol('#6b4a2c'), 'r': hexcol('#8a3a2a')}
SERVANT = [
	"...kkkkk....",
	"..kbbbbbk...",
	"..kbkgbgk...",
	"..kbbbkbk...",
	"...kbkbk....",
	"..kkBbBkk...",
	".kSskbkbkk..",
	"kSssSkbkbsk.",
	"kSssSkbbbsk.",
	"kSssSkbkk.s.",
	".kSSk.kbk...",
	"....kbk.kbk.",
	"....kbk.kbk.",
	"...kbbk.kbbk",
]
SERVANT_RUN = {11: "...kbk...kbk", 12: "..kbk.....kb", 13: "..kbbk....kk"}
SERVANT_SWING = {6: ".kSskbkbkks.", 7: "kSssSkbkbks.", 8: "kSssSkbbbs..", 9: "kSssSkbkk..."}


def servant_frame(pal, dy=0, run=False, swing=False, eyes=None):
	c = Canvas(12, 15)
	rows = SERVANT
	if run:
		rows = replace_rows(rows, SERVANT_RUN)
	if swing:
		rows = replace_rows(rows, SERVANT_SWING)
	c.grid(rows, pal, 0, 1 + dy)
	return c


def _servant_like(pal, death_col):
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return servant_frame(pal, dy=[0, 0, 1, 1][s])
		if m == 'run':
			return servant_frame(pal, run=s % 2 == 0, dy=-(s % 2))
		if m in ('attack', 'zap'):
			return [servant_frame(pal, swing=True), servant_frame(pal), servant_frame(pal, swing=True)][s]
		return death(servant_frame(pal), death_col)[s]
	return frames(draw)


def skeletal_servant():
	return _servant_like(SERVANT_PAL, '#5dff9a')


RISEN_PAL = dict(SERVANT_PAL)
RISEN_PAL.update({'b': hexcol('#9a9aa8'), 'B': hexcol('#5e5c70'), 'g': hexcol('#c08aff'),
                  's': hexcol('#4a3a5a'), 'S': hexcol('#2e2440')})


def risen():
	return _servant_like(RISEN_PAL, '#c08aff')


# ------------------------------------------------------------------ Homunculus 13x12
HOMUN_PAL = {'k': hexcol('#200c14'), 'p': hexcol('#e88a98'), 'P': hexcol('#b85a6a'), 'h': hexcol('#ffc0c8'),
             'w': hexcol('#ffffff'), 'e': hexcol('#3ad870'), 'x': hexcol('#6a2a3a'), 'g': hexcol('#9ad8c8')}
HOMUN = [
	"....kkkk.....",
	"...khhppk....",
	"..khpwwppk...",
	"..kpwkewpk...",
	"..kpwwwwpk...",
	"..kppxpxpPk..",
	".kpkppppPkpk.",
	".kpkPpPpPkpk.",
	"..k.kPPPk.k..",
	"....kPkPk....",
	"....kk.kk....",
]


def homun_frame(dy=0, blink=False, hop=False):
	c = Canvas(13, 12)
	rows = HOMUN
	if blink:
		rows = replace_rows(rows, {3: "..kpkkkkpk...", 4: "..kpppppk...."})
	if hop:
		rows = replace_rows(rows, {9: "....kPPPk....", 10: ".....kkk....."})
	c.grid(rows, HOMUN_PAL, 0, 1 + dy)
	return c


def homunculus():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [homun_frame(), homun_frame(dy=0), homun_frame(blink=True), homun_frame()][s]
		if m == 'run':
			return homun_frame(dy=-(s % 2), hop=s % 2 == 1)
		if m in ('attack', 'zap'):
			return [homun_frame(hop=True, dy=-1), homun_frame(), homun_frame(blink=True)][s]
		return death(homun_frame(), '#9ad8c8')[s]
	return frames(draw)


# ------------------------------------------------------------------ Shrines 14x16
SHRINE_BASE = {'k': hexcol('#121418'), 's': hexcol('#8a8f9a'), 'S': hexcol('#5a5e68'), 'h': hexcol('#b8bcc6')}
SHRINE = [
	"..............",
	"......kk......",
	".....kcCk.....",
	"....kcwcCk....",
	"....kccCCk....",
	".....kcCk.....",
	"......kk......",
	"..kkkkkkkkkk..",
	".khhhhhhhhssk.",
	".kSSSSSSSSSSk.",
	"..kshssssSk...",
	"..kshssssSk...",
	"..kshsCcsSk...",
	"..kshssssSk...",
	".kshhhssssSSk.",
	".kkkkkkkkkkkk.",
]
SHRINE_KINDS = {
	'blood': {'c': hexcol('#ff4a4a'), 'C': hexcol('#9a1a1a'), 'w': hexcol('#ffd0d0')},
	'gold':  {'c': hexcol('#ffd76a'), 'C': hexcol('#b8862a'), 'w': hexcol('#fff6d0')},
	'arcane': {'c': hexcol('#6af3ff'), 'C': hexcol('#2a6ad0'), 'w': hexcol('#e8ffff')},
}


def shrine(kind):
	pal = dict(SHRINE_BASE)
	pal.update(SHRINE_KINDS[kind])

	def f(dy=0, glow=0.0):
		c = Canvas(14, 16)
		c.grid(SHRINE[7:], pal, 0, 7)
		c.grid(SHRINE[:7], pal, 0, dy)
		if glow:
			c = c.recolor(lambda p: mix(p, pal['w'], glow) if p in (pal['c'], pal['C']) else p)
		return c

	def draw(p):
		m, s = p['mode'], p['step']
		if m in ('idle', 'run'):
			return f(dy=[0, -1, -1, 0][s], glow=[0, 0.3, 0.5, 0.2][s])
		if m in ('attack', 'zap'):
			return f(glow=[0.5, 1.0, 0.3][s])
		return death(f(), '#ffffff')[s]
	return frames(draw)


# ------------------------------------------------------------------ Golden altar 16x18
ALTAR_PAL = {'k': hexcol('#1a1006'), 'y': hexcol('#ffd76a'), 'Y': hexcol('#c8962e'), 'd': hexcol('#7a5414'),
             'l': hexcol('#fff2a8'), 's': hexcol('#8a8f9a'), 'S': hexcol('#5a5e68'), 'h': hexcol('#b8bcc6'),
             'r': hexcol('#e8473c'), 'c': hexcol('#6af3ff'), 'g': hexcol('#5dff9a')}
ALTAR = [
	"................",
	"..k....k....k...",
	".krk..kck..kgk..",
	"..k....k....k...",
	"................",
	"..kkkkkkkkkkkk..",
	".klllyyyyyyyYYk.",
	".kYYYYYYYYYYYdk.",
	"..kkkkkkkkkkkk..",
	"...khyyyyyysk...",
	"...khkkkkkksk...",
	"...khyyYyysSk...",
	"...khkkkkkksk...",
	"...khyyyyyysk...",
	"..khhhssssssSk..",
	".kkkkkkkkkkkkkk.",
]


def golden_altar():
	def f(dy=0, glow=0.0):
		c = Canvas(16, 18)
		c.grid(ALTAR[4:], ALTAR_PAL, 0, 2)
		# the three offerings float above the altar
		c.grid(ALTAR[:4], ALTAR_PAL, 0, dy + 1)
		if glow:
			c = c.recolor(lambda p: mix(p, hexcol('#fff6d0'), glow) if p in (ALTAR_PAL['y'], ALTAR_PAL['l']) else p)
		return c

	def draw(p):
		m, s = p['mode'], p['step']
		if m in ('idle', 'run'):
			return f(dy=[0, -1, -1, 0][s], glow=[0, 0.2, 0.35, 0.15][s])
		if m in ('attack', 'zap'):
			return f(glow=[0.4, 0.8, 0.3][s])
		return death(f(), '#ffd76a')[s]
	return frames(draw)
