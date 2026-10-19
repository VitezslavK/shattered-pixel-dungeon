"""Sewers: Sewer Leech, Rat Matriarch (alternative boss) and her brood."""
import math
from pix import Canvas, hexcol, mix, darker
from mobkit import frames, ramp, fade, flash, squash, legs

# ------------------------------------------------------------------ Sewer Leech, 16x12
BODY = ramp('#3b0b16', '#6a1526', '#952233', '#b93a43', '#d6615c')
SPOT, SPOT_D = hexcol('#e8973c'), hexcol('#a85a1f')
MOUTH, TOOTH, EYE, BLOOD = hexcol('#1c0306'), hexcol('#f1e6cf'), hexcol('#0d0d0d'), hexcol('#d0182a')


def leech(arch=0.0, rear=0.0, head_fwd=0.0, mouth=0, sq=1.0, fd=0.0):
	c = Canvas(16, 12)
	n = 11
	pts = []
	for i in range(n):
		t = i / (n - 1)
		x = 2.5 + t * (10.0 + head_fwd)
		y = 9.2 - arch * math.sin(math.pi * t) - rear * t ** 2.2
		r = (1.0 + 1.9 * math.sin(math.pi * (0.12 + 0.78 * t))) * sq
		pts.append((x, y + (2.9 * sq - r) * 0.6, r))
	for x, y, r in pts:
		c.blob(x, y, r, r, BODY)
	for y in range(12):
		for x in range(16):
			p = c.get(x, y)
			if p is not None and x % 3 == 1 and x < pts[-1][0] - 1:
				c.set(x, y, mix(p, BODY[0], 0.35))
	for x, y, r in pts[2:-2:2]:
		top = int(round(y - r * 0.6))
		c.set(x, top, SPOT)
		c.set(x + 1, top, SPOT_D)
	hx, hy, hr = pts[-1]
	c.set(hx - 0.5, hy - hr * 0.7, EYE)
	mx, my = int(round(hx + hr * 0.9)), int(round(hy))
	c.set(mx, my, MOUTH)
	if mouth:
		c.set(mx, my - 1, TOOTH)
		c.set(mx, my + 1, TOOTH)
		c.set(mx + 1, my, MOUTH)
		if mouth == 2:
			c.set(mx + 1, my - 1, TOOTH)
			c.set(mx + 1, my + 1, TOOTH)
			c.set(mx + 2, my, BLOOD)
	c.outline(factor=0.4)
	return fade(c, fd) if fd else c


def sewer_leech():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [leech(), leech(sq=1.06), leech(sq=1.06, mouth=1), leech(mouth=1)][s]
		if m == 'run':
			return [leech(arch=0.5), leech(arch=2.5, head_fwd=-1), leech(arch=3.5, head_fwd=-1.5), leech(arch=1.5, head_fwd=0.5)][s]
		if m in ('attack', 'zap'):
			return [leech(rear=3, mouth=1), leech(rear=1, head_fwd=1.5, mouth=2), leech(head_fwd=1, mouth=1)][s]
		return [leech(sq=0.85, mouth=1, fd=0.2), leech(sq=0.65, fd=0.45), leech(sq=0.5, fd=0.7), leech(sq=0.4, fd=0.9)][s]
	return frames(draw)


# ------------------------------------------------------------------ rats (hand drawn bodies)
from mobkit import body_frame, replace_rows

RAT_PAL = {
	'k': hexcol('#1e1414'), 'd': hexcol('#4a3636'), 'f': hexcol('#6e5250'), 'F': hexcol('#8f716c'),
	'h': hexcol('#b0918a'), 'b': hexcol('#c8aaa0'), 'p': hexcol('#e08a96'), 'P': hexcol('#a85866'),
	'e': hexcol('#ff3b3b'), 'y': hexcol('#e8c25a'), 'Y': hexcol('#a8822a'), 'w': hexcol('#f4ecd8'),
	'r': hexcol('#d0182a'), 'm': hexcol('#2a0808'),
}

# Rat Matriarch, 24x17: a huge hunched rat queen with a bone-gold crown and a scarred back
MATRIARCH = [
	"................y.y.y...",
	"...............kyyyyyk..",
	"........kkkkk..kYrYYk...",
	"......kkhhhFFkkkFFFFk...",
	".....khhFFFFFFkpkFFFFk..",
	"....khFFFkFFFFFkFFFeFFk.",
	"...khFFFFFkFFFFFFFFFFFpk",
	"..khFFFFFFkFFFFFFFFFFkk.",
	"..kFFFFFFFFFFFFFfFFFmwk.",
	"p.kfFFFFFFFFFFFfffkkk...",
	"pkkffFFFFFFFFFfffk......",
	".pkfffbbbbbbffffk.......",
	"..kdffffffffffddk.......",
	"...kkddddddddddkk.......",
]
MATRIARCH_OPEN = {7: "..khFFFFFFkFFFFFFFFFFkmk", 8: "..kFFFFFFFFFFFFFfFFFmmwk", 9: "p.kfFFFFFFFFFFFfffkkwk.."}
MATRIARCH_LEGS = {
	'stand': ["....kdk.kdk...kdk.kdk...", "....kdk.kdk...kdk.kdk...", "....kkk.kkk...kkk.kkk..."],
	'run1':  ["...kdk...kdk...kdkkdk...", "..kdk.....kdk.kdk..kdk..", "..kk.......kk.kk....kk.."],
	'run2':  ["....kdkkdk.....kdkdk....", "....kdkkdk.....kdkdk....", "....kkkkkk.....kkkkk...."],
}


def rat_matriarch():
	W, H = 24, 17

	def m(legs='stand', dx=0, dy=0, mouth=False):
		rows = replace_rows(MATRIARCH, MATRIARCH_OPEN) if mouth else MATRIARCH
		c = Canvas(W, H)
		c.grid(MATRIARCH_LEGS[legs], RAT_PAL, 0, 14)
		c.grid(rows, RAT_PAL, dx, dy)
		return c

	def draw(p):
		mo, s = p['mode'], p['step']
		if mo == 'idle':
			return [m(), m(dy=1), m(dy=1), m()][s]
		if mo == 'run':
			return [m('run1'), m('run2', dy=1), m('run1'), m('run2', dy=1)][s]
		if mo == 'attack':
			return [m(dx=-1), m(dx=1, mouth=True), m(dx=1, mouth=True)][s]
		if mo == 'zap':
			return [m(dy=-1, mouth=True), flash(m(dy=-1, mouth=True), hexcol('#ffd0d0'), 0.3), m(mouth=True)][s]
		base = m(mouth=True)
		return [base, fade(squash(base, 0.85), 0.3), fade(squash(base, 0.6), 0.55), fade(squash(base, 0.4), 0.85)][s]
	return frames(draw)


BROOD_PAL = dict(RAT_PAL)
BROOD_PAL.update({'d': hexcol('#9a5a62'), 'f': hexcol('#c27a80'), 'F': hexcol('#dc9a98'), 'h': hexcol('#f2c0b8'),
                  'b': hexcol('#f8dcd4'), 'e': hexcol('#1a0606')})

# Matriarch's brood, 13x10: hairless pink pups, all head and tail
BROOD = [
	".......kk....",
	"..kkkkkpkkk..",
	".khhFFkPFFFk.",
	"kFFFFFFFFeFFk",
	"kfFFFFFFFFFpk",
	"kffbbbbFFfkk.",
	".kkffffffk...",
]
BROOD_OPEN = {4: "kfFFFFFFFFmwk", 5: "kffbbbbFFfmk."}
BROOD_LEGS = {
	'stand': ["pkdk..kdk....", "..kk..kk....."],
	'run1':  ["pkdk...kdk...", ".kk.....kk..."],
	'run2':  ["p.kdkkdk.....", "..kk.kk......"],
}


def brood():
	W, H = 13, 10

	def b(legs='stand', dx=0, dy=0, mouth=False):
		rows = replace_rows(BROOD, BROOD_OPEN) if mouth else BROOD
		c = Canvas(W, H)
		c.grid(BROOD_LEGS[legs], BROOD_PAL, 0, 7 + dy)
		c.grid(rows, BROOD_PAL, dx, dy + 1)
		return c

	def draw(p):
		mo, s = p['mode'], p['step']
		if mo == 'idle':
			return [b(), b(dy=-0), b(mouth=True), b()][s]
		if mo == 'run':
			return [b('run1'), b('run2'), b('run1'), b('run2')][s]
		if mo in ('attack', 'zap'):
			return [b(dx=-1), b(dx=1, mouth=True), b(mouth=True)][s]
		base = b()
		return [base, fade(squash(base, 0.8), 0.3), fade(squash(base, 0.6), 0.6), fade(squash(base, 0.4), 0.9)][s]
	return frames(draw)
