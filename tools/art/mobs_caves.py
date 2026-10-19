"""Caves: Crystal Crawler, Crystal Queen (alternative boss), Brass Golem."""
import math
from pix import Canvas, hexcol, mix
from mobkit import frames, fade, flash, squash, replace_rows

CRAWLER_PAL = {
	'k': hexcol('#10141c'), 'd': hexcol('#22303e'), 'f': hexcol('#3a5064'), 'F': hexcol('#56728c'),
	'h': hexcol('#7e9ab2'), 'c': hexcol('#8af2ff'), 'C': hexcol('#2aa8d8'), 'w': hexcol('#eaffff'),
	'e': hexcol('#ffe86a'),
}

# Crystal Crawler, 16x13: a squat cave crab whose shell has grown crystal spikes
CRAWLER = [
	"......w.........",
	".....kck...w....",
	"..w..kcCk.kck...",
	".kck.kCCkkcCk...",
	".kCCkkCCkkCCk.e.",
	"..kkkhhFFFFkk.k.",
	".khhFFFFFFFFFkk.",
	"khFFFfFFFFFFFFk.",
	"kfffFFFFFFFFFkCk",
	".kffffffffffkcCk",
	"..kkkkkkkkkkkCCk",
]
CRAWLER_PINCH = {8: "kfffFFFFFFFFFkCk", 9: ".kffffffffffkc.k", 10: "..kkkkkkkkkkkCk."}
CRAWLER_LEGS = {
	'stand': ["..kdk.kdk.kdk...", ".kd..kd...kd...."],
	'run1':  ["..kdk..kdk.kdk..", "..kd...kd...kd.."],
	'run2':  [".kdk.kdk.kdk....", "kd..kd...kd....."],
}


def crystal_crawler():
	W, H = 16, 13

	def c_(legs='stand', dx=0, dy=0, pinch=False, sparkle=0):
		c = Canvas(W, H)
		c.grid(CRAWLER_LEGS[legs], CRAWLER_PAL, dx, 11)
		c.grid(replace_rows(CRAWLER, CRAWLER_PINCH) if pinch else CRAWLER, CRAWLER_PAL, dx, dy)
		if sparkle:
			for x, y in ((6, 0), (11, 1), (2, 2))[:sparkle]:
				c.set(x, y + dy, CRAWLER_PAL['w'])
		return c

	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [c_(), c_(sparkle=1), c_(sparkle=0), c_(sparkle=3)][s]
		if m == 'run':
			return [c_('run1'), c_('run2', dy=0), c_('run1', dx=1), c_('run2')][s]
		if m in ('attack', 'zap'):
			return [c_(pinch=True), c_(dx=1, pinch=False), c_(dx=1, pinch=True)][s]
		base = c_()
		return [flash(base, hexcol('#ffffff'), 0.4), fade(squash(base, 0.8), 0.3), fade(squash(base, 0.55), 0.6), fade(squash(base, 0.35), 0.9)][s]
	return frames(draw)


QUEEN_PAL = {
	'k': hexcol('#1c0a22'), 'q': hexcol('#6a2478'), 'P': hexcol('#b04cbc'), 'p': hexcol('#ec8ae0'),
	'w': hexcol('#fff0ff'), 'y': hexcol('#ffd76a'), 'Y': hexcol('#b88a2a'), 'e': hexcol('#ffffff'),
}

# Crystal Queen, 20x22: a floating figure cut from rose crystal, crowned, trailing shards
QUEEN = [
	".......y..y..y......",
	".......yYyyyYy......",
	"......kkkkkkkkk.....",
	"......kpwpppPqk.....",
	"......kpwkpkPqk.....",
	"......kppppPPqk.....",
	".......kpPPqqk......",
	"....kk..kPqk..kk....",
	"...kpwk.kPqk.kpPk...",
	"...kpPkkpwpPkkPqk...",
	"....kkkpwppPPkkk....",
	"......kpwpppPqk.....",
	"......kppppPPqk.....",
	".......kpppPqk......",
	".......kppPPqk......",
	"........kpPqk.......",
	"........kpPqk.......",
	".........kqk........",
	"..........k.........",
]


def queen_frame(bob=0, orbit=0.0, glow=0.0, open_arms=False):
	W, H = 20, 22
	c = Canvas(W, H)
	rows = QUEEN
	if open_arms:
		rows = replace_rows(QUEEN, {7: "..kk....kPqk....kk..", 8: ".kpwk...kPqk...kpPk.", 9: ".kpPkkkkpwpPkkkkPqk."})
	c.grid(rows, QUEEN_PAL, 0, 1 + bob)
	# three shards orbiting the body
	for i in range(3):
		a = orbit + i * 2.094
		x = 10 + math.cos(a) * 8.5
		y = 12 + bob + math.sin(a) * 3
		if math.sin(a) < -0.3:
			continue  # behind the body
		c.set(x, y, QUEEN_PAL['p'])
		c.set(x, y - 1, QUEEN_PAL['w'])
		c.set(x, y + 1, QUEEN_PAL['P'])
	if glow:
		c = c.recolor(lambda px: mix(px, hexcol('#ffd0ff'), glow))
	return c


def crystal_queen():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return queen_frame(bob=[0, 0, 1, 1][s], orbit=s * 1.57)
		if m == 'run':
			return queen_frame(bob=[0, 1, 1, 0][s], orbit=s * 1.57 + 0.7)
		if m in ('attack', 'zap'):
			return [queen_frame(glow=0.2, open_arms=True), queen_frame(glow=0.5, open_arms=True, orbit=1), queen_frame(open_arms=True, orbit=2)][s]
		base = queen_frame(glow=0.3)
		return [flash(base, hexcol('#ffffff'), 0.5), fade(base, 0.35), fade(squash(base, 0.7), 0.65), fade(squash(base, 0.4), 0.9)][s]
	return frames(draw)


GOLEM_PAL = {
	'k': hexcol('#1c1208'), 'd': hexcol('#5c3a14'), 'b': hexcol('#8c5e22'), 'B': hexcol('#ba8a3a'),
	'y': hexcol('#e8c060'), 'w': hexcol('#fff2c0'), 'o': hexcol('#ff8a20'), 'O': hexcol('#ffd060'),
	'g': hexcol('#5e5e66'), 'G': hexcol('#9a9aa4'), 's': hexcol('#d8d8e0'),
}

# Brass Golem, 18x20: a clockwork furnace on legs, gear in the chest, steam from the shoulder pipe
GOLEM = [
	"..............kk..",
	".....kkkkkk..kGk..",
	"....kByyyyBk.kGk..",
	"....kBkookBkkkGk..",
	"....kBBBBBBkBBBk..",
	"..kkkdbbbbdkkBBk..",
	".kBBykkkkkkyBBk...",
	"kByyBkGGGGkBByBk..",
	"kBBBkGgsgGGkBBBk..",
	"kbbBkGsOsgGkBbbk..",
	"kbbBkGgsgGGkBbbk..",
	"kddbkkGGGGkkbddk..",
	"kyyk.kbbbbk.kyyk..",
	"kkkk.kbBBbk.kkkk..",
	".....kbbbbk.......",
]
GOLEM_LEGS = {
	'stand': ["....kbBk.kBbk.....", "....kbBk.kBbk.....", "...kkyyk.kyykk....", "...kkkkk.kkkkk....", "..................", ".................."],
	'run1':  ["...kbBk...kBbk....", "...kbBk....kBbk...", "..kkyyk....kyykk..", "..kkkkk....kkkkk..", "..................", ".................."],
	'run2':  [".....kbBkBbk......", ".....kbBkBbk......", "....kkyykyykk.....", "....kkkkkkkkk.....", "..................", ".................."],
}


def golem_frame(legs='stand', dy=0, puff=0, eyes='o', punch=False):
	W, H = 18, 20
	c = Canvas(W, H)
	c.grid(GOLEM_LEGS[legs], GOLEM_PAL, 0, 15)
	rows = GOLEM
	if eyes != 'o':
		rows = replace_rows(rows, {3: "....kBk" + eyes * 2 + "kBkkkGk.."})
	if punch:
		rows = replace_rows(rows, {9: "kbbBkGsOsgGkBbbBBk", 10: "kbbBkGgsgGGkBkkyyk", 11: "kddbkkGGGGkkbk.kkk", 12: "kyyk.kbbbbk.k.....", 13: "kkkk.kbBBbk......."})
	c.grid(rows, GOLEM_PAL, 0, dy)
	steam = hexcol('#e8e8ee', 200)
	for i in range(puff):
		c.set(15 + (i % 2), -1 - i + dy + 1, steam)
	return c


def brass_golem():
	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [golem_frame(), golem_frame(puff=1), golem_frame(dy=0, puff=2, eyes='O'), golem_frame()][s]
		if m == 'run':
			return [golem_frame('run1'), golem_frame('run2', dy=1, puff=1), golem_frame('run1'), golem_frame('run2', dy=1)][s]
		if m == 'attack':
			return [golem_frame(eyes='O', puff=2), golem_frame(punch=True, eyes='O'), golem_frame(punch=True)][s]
		if m == 'zap':
			return [golem_frame(eyes='O', puff=1), flash(golem_frame(eyes='O', puff=2), hexcol('#ffd060'), 0.4), golem_frame(puff=1)][s]
		base = golem_frame(eyes='kk'[0])
		return [flash(golem_frame(), hexcol('#ffffff'), 0.4), fade(squash(base, 0.85), 0.3), fade(squash(base, 0.6), 0.6), fade(squash(base, 0.4), 0.9)][s]
	return frames(draw)
