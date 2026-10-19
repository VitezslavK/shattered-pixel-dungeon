"""Prison: Prison Hound and the Jailer (alternative boss)."""
from pix import Canvas, hexcol, mix
from mobkit import frames, fade, flash, squash, replace_rows

HOUND_PAL = {
	'k': hexcol('#121216'), 'd': hexcol('#2e2c33'), 'f': hexcol('#46424c'), 'F': hexcol('#625c68'),
	'h': hexcol('#88828e'), 's': hexcol('#c4c8d0'), 'S': hexcol('#6e727c'), 'e': hexcol('#ffd040'),
	'w': hexcol('#f0ead8'), 'm': hexcol('#3a0a10'),
}

# Prison Hound, 18x14: a starved guard dog, ribs showing, iron collar with a torn chain
HOUND = [
	"............kk....",
	"kk.........kFk....",
	".kk.......kFhFkkk.",
	"..k.....kksFFFFFFk",
	"..kkkkkkkSsFFeFFFk",
	".khhFFFFkSsFFFFFkk",
	".kFdFdFFFkSFFFkkk.",
	".kFFdFdFFFsSkkk...",
	"..kfFFFFFFFSk.....",
	"..kffkkkkffks.....",
]
HOUND_SNARL = {5: ".khhFFFFkSsFFFFFwk", 6: ".kFdFdFFFkSFFFmwk.", 7: ".kFFdFdFFFsSkwkk.."}
HOUND_LEGS = {
	'stand': ["..kfkk..kfkk......", "..kfkfk.kfkfk.....", "..kfkfk.kfkfk.....", "..kkkkk.kkkkk....."],
	'run1':  [".kfkk...kfkk......", "kfk.kfkkfk.kfk....", "kk...kkkk....kk...", ".................."],
	'run2':  ["..kfkk..kfkk......", "...kfk...kfk......", "...kfk...kfk......", "...kk....kk......."],
	'run3':  ["..kfkk..kfkk......", "..kfk.kk.kfkk.....", ".kfk...kkk..kk....", ".kk..............."],
}


def prison_hound():
	W, H = 18, 14

	def d(legs='stand', dx=0, dy=0, snarl=False):
		c = Canvas(W, H)
		c.grid(HOUND_LEGS[legs], HOUND_PAL, 0, 10 + dy)
		c.grid(replace_rows(HOUND, HOUND_SNARL) if snarl else HOUND, HOUND_PAL, dx, dy)
		return c

	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [d(), d(snarl=True), d(snarl=True), d()][s]
		if m == 'run':
			return [d('run1', dy=-1), d('run2'), d('run3', dy=-1), d('run2')][s]
		if m in ('attack', 'zap'):
			return [d(dx=-1, snarl=True), d('run1', dx=1, dy=-1, snarl=True), d(dx=1, snarl=True)][s]
		base = d()
		return [flash(base, hexcol('#ffffff'), 0.3), fade(squash(base, 0.8), 0.3), fade(squash(base, 0.55), 0.6), fade(squash(base, 0.35), 0.9)][s]
	return frames(draw)


JAILER_PAL = {
	'k': hexcol('#121218'), 'S': hexcol('#4e5562'), 's': hexcol('#8a93a3'), 'w': hexcol('#c9d0dc'),
	'r': hexcol('#ff3a2a'), 'L': hexcol('#4a3424'), 'l': hexcol('#6e4e34'), 'y': hexcol('#e0b04a'),
	'Y': hexcol('#8a6a2a'), 'c': hexcol('#9aa0aa'), 'b': hexcol('#2a1c14'),
}

# The Jailer, 20x22: a hulking warden in an iron helm, keys at the belt, a chained iron ball in hand
JAILER = [
	"......kkkkkk........",
	".....kSsswsSk.......",
	"....kSsswwssSk......",
	"....kSkkkkkkSk......",
	"....kSkrkkrkSk......",
	"....kSSkkkkSSk......",
	"...kkkSSSSSSkkk.....",
	"..kSswkllllkSswk....",
	".kSsswkLlllLkSswk...",
	".kSSSkLLlllLLkSSk...",
	"..kkkLLLLsLLLLkkk...",
	"..kskLLLLLLLLLkSk...",
	"..kSkbbbbybbbbkSk...",
	"..kkkLyYLLLLLLkkk...",
	"....kLyLLLLLLLk.....",
	"....kLLLLkLLLLk.....",
]
JAILER_LEGS = {
	'stand': ["....kSSk.kSSk.......", "....kSSk.kSSk.......", "....kssk.kssk.......", "...kkkkk.kkkkk......", "....................", "...................."],
	'run1':  ["...kSSk...kSSk......", "...kSSk....kSSk.....", "..kssk......kssk....", "..kkkk......kkkkk...", "....................", "...................."],
	'run2':  [".....kSSkSSk........", ".....kSSkSSk........", ".....ksskssk........", "....kkkkkkkkk.......", "....................", "...................."],
}


def flail(c, hx, hy, bx, by):
	"""chain from the hand to an iron ball"""
	chain, ball, ball_l = JAILER_PAL['c'], JAILER_PAL['S'], JAILER_PAL['s']
	n = max(abs(bx - hx), abs(by - hy))
	for i in range(1, n):
		t = i / n
		c.set(hx + (bx - hx) * t, hy + (by - hy) * t, chain if i % 2 else JAILER_PAL['k'])
	for dy in (-1, 0, 1):
		for dx in (-1, 0, 1):
			c.set(bx + dx, by + dy, ball)
	c.set(bx - 1, by - 1, ball_l)
	c.set(bx, by - 2, JAILER_PAL['k'])
	c.set(bx + 2, by, JAILER_PAL['k'])
	c.set(bx - 2, by, JAILER_PAL['k'])
	c.set(bx, by + 2, JAILER_PAL['k'])


def jailer():
	W, H = 20, 22

	def j(legs='stand', dy=0, ball=(17, 18), glow=0.0):
		c = Canvas(W, H)
		c.grid(JAILER_LEGS[legs], JAILER_PAL, 0, 16)
		c.grid(JAILER, JAILER_PAL, 0, dy)
		flail(c, 16, 12 + dy, ball[0], ball[1])
		if glow:
			c.set(7, 4 + dy, mix(JAILER_PAL['r'], hexcol('#ffffff'), glow))
			c.set(10, 4 + dy, mix(JAILER_PAL['r'], hexcol('#ffffff'), glow))
		return c

	def draw(p):
		m, s = p['mode'], p['step']
		if m == 'idle':
			return [j(), j(ball=(17, 19)), j(dy=1, ball=(17, 19)), j(dy=1)][s]
		if m == 'run':
			return [j('run1', ball=(18, 17)), j('run2', dy=1, ball=(16, 19)), j('run1', ball=(18, 17)), j('run2', dy=1, ball=(16, 19))][s]
		if m == 'attack':
			return [j(ball=(14, 2), glow=0.4), j(ball=(18, 6), glow=0.6), j(ball=(18, 19))][s]
		if m == 'zap':
			# throws the chain out to drag someone in
			return [j(ball=(12, 3), glow=0.5), j(ball=(19, 10), glow=0.8), j(ball=(18, 15))][s]
		base = j(ball=(18, 20))
		return [flash(base, hexcol('#ffffff'), 0.35), fade(squash(base, 0.85), 0.3), fade(squash(base, 0.6), 0.6), fade(squash(base, 0.4), 0.9)][s]
	return frames(draw)
