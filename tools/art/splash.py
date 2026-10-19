"""Hero select splash pictures (800x450): a 200x112 pixel scene enlarged 4x.
Phones show only the middle quarter, so the hero and the important props sit near the centre.
Each hero stands in the middle of their own place: the Necromancer in a misty graveyard, the Arcanist
in an old library under a floating rune circle, the Alchemist in a workshop beside a cauldron."""
import math
import random
from PIL import Image
from pix import Canvas, hexcol, mix
import heroes

W, H = 200, 112


def gradient(c, top, bottom):
	for y in range(H):
		col = mix(top, bottom, y / (H - 1))
		for x in range(W):
			c.set(x, y, col)


def glow(c, cx, cy, r, col, strength=0.6):
	for y in range(int(cy - r), int(cy + r) + 1):
		for x in range(int(cx - r), int(cx + r) + 1):
			d = math.hypot(x - cx, y - cy) / r
			p = c.get(x, y)
			if p is None or d >= 1:
				continue
			t = (1 - d) ** 2 * strength
			# dither the falloff a little so it reads as pixel art
			if (x + y) % 2 and t < 0.12:
				continue
			c.set(x, y, mix(p, col, t))


def paste_big(c, small, ox, oy, scale):
	for y in range(small.h):
		for x in range(small.w):
			p = small.get(x, y)
			if p is None:
				continue
			for dy in range(scale):
				for dx in range(scale):
					c.set(ox + x * scale + dx, oy + y * scale + dy, p)


def figure(cls):
	"""the hero's portrait at 3x, shaded with a rim light on the right side"""
	return heroes.avatar(cls)


def floor(c, y0, col, col2, rnd):
	for y in range(y0, H):
		for x in range(W):
			n = rnd.random()
			c.set(x, y, col2 if n < 0.18 else col)
	for x in range(W):
		c.set(x, y0, mix(col, (255, 255, 255, 255), 0.12))


def necromancer():
	rnd = random.Random(7)
	c = Canvas(W, H)
	gradient(c, hexcol('#0d1014'), hexcol('#1c2a24'))
	# moon and distant dead trees
	glow(c, 160, 18, 26, hexcol('#9fe7c0'), 0.35)
	for y in range(12, 25):
		for x in range(153, 168):
			if math.hypot(x - 160, y - 18) < 6:
				c.set(x, y, hexcol('#d7f5e2'))
	for tx in (18, 52, 128, 182):
		h = rnd.randint(28, 40)
		for i in range(h):
			c.set(tx, 82 - i, hexcol('#0b0f0e'))
			if i > h * 0.5 and i % 5 == 0:
				d = rnd.choice((-1, 1))
				for j in range(rnd.randint(3, 7)):
					c.set(tx + d * j, 82 - i - j // 2, hexcol('#0b0f0e'))
	floor(c, 82, hexcol('#1f2a22'), hexcol('#17201a'), rnd)
	# gravestones
	stone, stone_d = hexcol('#55606a'), hexcol('#3a434b')
	for gx, gh in ((30, 14), (78, 11), (124, 13), (170, 10)):
		for y in range(82 - gh, 83):
			for x in range(gx - 4, gx + 5):
				top = 82 - gh
				if y < top + 3 and abs(x - gx) > 4 - (y - top):
					continue
				c.set(x, y, stone if x < gx + 2 else stone_d)
		c.line(gx, 82 - gh + 4, gx, 82 - gh + 8, stone_d)
		c.line(gx - 2, 82 - gh + 5, gx + 2, 82 - gh + 5, stone_d)
	# ground mist and rising souls
	for i in range(240):
		x, y = rnd.randint(0, W - 1), rnd.randint(76, 100)
		p = c.get(x, y)
		c.set(x, y, mix(p, hexcol('#8fd6b0'), 0.25))
	glow(c, 100, 70, 30, hexcol('#3cff8a'), 0.35)
	for sx, sy in ((86, 50), (114, 44), (90, 34), (118, 62), (108, 26)):
		glow(c, sx, sy, 4, hexcol('#5dff9a'), 0.9)
		c.set(sx, sy, hexcol('#d8ffe8'))
	paste_big(c, figure(heroes.Necromancer), 100 - 12, 86 - 32, 1)
	return c


def arcanist():
	rnd = random.Random(3)
	c = Canvas(W, H)
	gradient(c, hexcol('#0c0f22'), hexcol('#1a1e3a'))
	# bookshelves on both sides
	wood, wood_d = hexcol('#4a3020'), hexcol('#2e1d13')
	book_cols = [hexcol(h) for h in ('#7a2a2a', '#2a4f7a', '#2f6b3f', '#7a6a2a', '#5a2f6b', '#8a5a3a')]
	for x0 in (0, 150):
		c.rect(x0, 10, x0 + 49, 90, wood_d)
		for shelf in range(4):
			y = 14 + shelf * 19
			c.rect(x0 + 2, y + 15, x0 + 47, y + 16, wood)
			x = x0 + 3
			while x < x0 + 46:
				w = rnd.randint(2, 3)
				h = rnd.randint(9, 14)
				col = rnd.choice(book_cols)
				c.rect(x, y + 15 - h, x + w - 1, y + 14, col)
				c.set(x, y + 15 - h + 2, mix(col, hexcol('#e6c25a'), 0.6))
				x += w + (1 if rnd.random() < 0.2 else 0)
	floor(c, 90, hexcol('#2a2440'), hexcol('#211c34'), rnd)
	# rune circle hovering behind the hero
	cyan, gold = hexcol('#5ef2ff'), hexcol('#ffd76a')
	glow(c, 100, 72, 30, hexcol('#3aa8ff'), 0.4)
	for a in range(0, 360, 2):
		r = math.radians(a)
		c.set(100 + 22 * math.cos(r), 72 + 22 * math.sin(r), cyan)
		c.set(100 + 18 * math.cos(r), 72 + 18 * math.sin(r), mix(cyan, hexcol('#1a1e3a'), 0.4))
	for i in range(8):
		r = math.radians(i * 45 + 10)
		x, y = 100 + 20 * math.cos(r), 72 + 20 * math.sin(r)
		for dx, dy in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
			c.set(x + dx, y + dy, gold)
	# floating motes
	for i in range(30):
		x, y = rnd.randint(70, 130), rnd.randint(30, 88)
		c.set(x, y, rnd.choice((cyan, gold)))
	paste_big(c, figure(heroes.Arcanist), 100 - 12, 90 - 32, 1)
	return c


def alchemist():
	rnd = random.Random(11)
	c = Canvas(W, H)
	gradient(c, hexcol('#1a120c'), hexcol('#2a1c12'))
	# stone wall
	brick, brick_d = hexcol('#3a2a20'), hexcol('#2a1d16')
	for y in range(0, 88, 6):
		off = 0 if (y // 6) % 2 else 6
		for x in range(-12 + off, W, 12):
			c.rect(x + 1, y + 1, x + 10, y + 4, brick)
			c.line(x + 1, y + 4, x + 10, y + 4, brick_d)
	# shelves with flasks
	glass = [hexcol(h) for h in ('#e0473c', '#55d46b', '#4f8ff0', '#f0c040', '#c060e0')]
	for sx, sy in ((8, 30), (150, 24), (80, 46)):
		c.rect(sx, sy, sx + 42, sy + 1, hexcol('#6b4a2c'))
		x = sx + 2
		while x < sx + 40:
			col = rnd.choice(glass)
			h = rnd.randint(4, 7)
			c.rect(x, sy - h, x + 2, sy - 1, col)
			c.set(x + 1, sy - h - 1, hexcol('#c9bea3'))
			c.set(x, sy - h + 1, mix(col, hexcol('#ffffff'), 0.5))
			x += 5
	floor(c, 88, hexcol('#3a2c22'), hexcol('#30241b'), rnd)
	# cauldron with a green glow and bubbles
	iron, iron_d = hexcol('#3c3f4a'), hexcol('#24262d')
	for y in range(78, 96):
		for x in range(102, 129):
			if ((x - 115) / 13) ** 2 + ((y - 86) / 9) ** 2 <= 1:
				c.set(x, y, iron if x < 117 else iron_d)
	c.rect(103, 77, 127, 79, hexcol('#5d6270'))
	c.rect(105, 75, 125, 77, hexcol('#6fe08a'))
	glow(c, 115, 70, 18, hexcol('#6fe08a'), 0.45)
	for bx, by in ((110, 70), (117, 64), (121, 71), (113, 58), (119, 52)):
		c.set(bx, by, hexcol('#d8ffe0'))
		c.set(bx + 1, by, hexcol('#6fe08a'))
	# fire under the cauldron
	for x in range(108, 123):
		h = rnd.randint(1, 4)
		for y in range(96, 96 + h):
			c.set(x, y, hexcol('#f0a030') if y < 97 else hexcol('#e0473c'))
	paste_big(c, figure(heroes.Alchemist), 88 - 12, 92 - 32, 1)
	return c


def save(c, path):
	# must stay 800x450: the hero select screen sets its zoom from the first splash's size
	im = c.image().convert('RGB').resize((W * 4, H * 4), Image.NEAREST)
	out = Image.new('RGB', (800, 450), (0, 0, 0))
	out.paste(im, (0, 1))
	out.save(path, quality=92)
