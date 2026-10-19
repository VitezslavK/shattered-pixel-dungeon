"""Tiny pixel-art toolkit used to draw the mod's own sprites.

Sprites are drawn at 1:1 pixel size, either from text grids (one character per pixel, mapped through a
palette) or from shaded shapes (discs/capsules lit from the top left), then finished with an outline
pass. Frames are packed left to right into sheets the game loads with TextureFilm.

Needs Pillow: python3 -m venv ~/.spd-build/artenv && ~/.spd-build/artenv/bin/pip install pillow
"""
import math
from PIL import Image

LIGHT = (-0.6, -0.8)  # light comes from the top left


def hexcol(h, a=255):
	h = h.lstrip('#')
	return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def mix(c1, c2, t):
	return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(4))


def darker(c, f=0.55):
	return (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])


class Canvas:
	def __init__(self, w, h):
		self.w, self.h = w, h
		self.px = [[None] * w for _ in range(h)]

	def copy(self):
		c = Canvas(self.w, self.h)
		c.px = [row[:] for row in self.px]
		return c

	def set(self, x, y, col):
		x, y = int(round(x)), int(round(y))
		if 0 <= x < self.w and 0 <= y < self.h:
			self.px[y][x] = col

	def get(self, x, y):
		if 0 <= x < self.w and 0 <= y < self.h:
			return self.px[y][x]
		return None

	def grid(self, rows, pal, ox=0, oy=0):
		"""draws a text grid, '.' and ' ' are transparent"""
		for y, row in enumerate(rows):
			for x, ch in enumerate(row):
				if ch in '. ':
					continue
				self.set(ox + x, oy + y, pal[ch])

	def rect(self, x0, y0, x1, y1, col):
		for y in range(y0, y1 + 1):
			for x in range(x0, x1 + 1):
				self.set(x, y, col)

	def line(self, x0, y0, x1, y1, col):
		n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
		for i in range(n):
			t = i / max(1, n - 1)
			self.set(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, col)

	def blob(self, cx, cy, rx, ry, ramp, light=LIGHT, rot=0.0):
		"""a filled, shaded ellipse. ramp goes from darkest to lightest"""
		cr, sr = math.cos(rot), math.sin(rot)
		for y in range(int(cy - max(rx, ry)) - 1, int(cy + max(rx, ry)) + 2):
			for x in range(int(cx - max(rx, ry)) - 1, int(cx + max(rx, ry)) + 2):
				dx, dy = x + 0.5 - cx, y + 0.5 - cy
				u = (dx * cr + dy * sr) / rx
				v = (-dx * sr + dy * cr) / ry
				d = u * u + v * v
				if d > 1.0:
					continue
				nz = math.sqrt(max(0.0, 1 - d))
				# lambert with a bit of ambient, in screen space
				nx = (u * cr - v * sr)
				ny = (u * sr + v * cr)
				lit = max(0.0, (nx * light[0] + ny * light[1]) * 0.7 + nz * 0.5)
				i = min(len(ramp) - 1, int(lit * len(ramp)))
				self.set(x, y, ramp[i])

	def outline(self, col=None, corners=False, factor=0.45):
		"""adds a 1px outline around everything drawn so far, tinted from the neighbouring colour"""
		add = []
		for y in range(self.h):
			for x in range(self.w):
				if self.px[y][x] is not None:
					continue
				nb = [(1, 0), (-1, 0), (0, 1), (0, -1)]
				if corners:
					nb += [(1, 1), (-1, 1), (1, -1), (-1, -1)]
				for dx, dy in nb:
					c = self.get(x + dx, y + dy)
					if c is not None and c[3] > 0:
						add.append((x, y, col if col else darker(c, factor)))
						break
		for x, y, c in add:
			self.px[y][x] = c

	def flip(self):
		c = Canvas(self.w, self.h)
		c.px = [row[::-1] for row in self.px]
		return c

	def shifted(self, dx, dy):
		c = Canvas(self.w, self.h)
		for y in range(self.h):
			for x in range(self.w):
				if self.px[y][x] is not None:
					c.set(x + dx, y + dy, self.px[y][x])
		return c

	def recolor(self, fn):
		c = Canvas(self.w, self.h)
		c.px = [[fn(p) if p is not None else None for p in row] for row in self.px]
		return c

	def image(self):
		im = Image.new('RGBA', (self.w, self.h), (0, 0, 0, 0))
		for y in range(self.h):
			for x in range(self.w):
				if self.px[y][x] is not None:
					im.putpixel((x, y), self.px[y][x])
		return im


def sheet(frames, path, width=None):
	"""packs frames left to right (wrapping at width) into one png"""
	fw, fh = frames[0].w, frames[0].h
	per_row = (width // fw) if width else len(frames)
	rows = (len(frames) + per_row - 1) // per_row
	w = width if width else fw * len(frames)
	im = Image.new('RGBA', (w, fh * rows), (0, 0, 0, 0))
	for i, f in enumerate(frames):
		im.paste(f.image(), ((i % per_row) * fw, (i // per_row) * fh))
	im.save(path)
	return im


def preview(images, path, scale=6, bg=(38, 38, 46, 255), gap=4):
	"""lays out images (PIL) side by side, enlarged, for checking designs"""
	w = sum(i.width for i in images) + gap * (len(images) + 1)
	h = max(i.height for i in images) + gap * 2
	out = Image.new('RGBA', (w, h), bg)
	x = gap
	for i in images:
		out.alpha_composite(i, (x, gap))
		x += i.width + gap
	out.resize((w * scale, h * scale), Image.NEAREST).save(path)
