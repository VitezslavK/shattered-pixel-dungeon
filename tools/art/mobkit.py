"""Shared helpers for monster sheets.

Every monster sheet uses the same 18-frame layout (see sprites/ModMobFrames.java):
  0-3 idle, 4-7 run, 8-10 attack, 11-14 die, 15-17 zap/cast (copies of attack if unused)
A creature is a function draw(pose) -> Canvas; pose is a dict with keys
  phase (0-3), mode ('idle','run','attack','die','zap'), step (index inside the mode)."""
import math
from pix import Canvas, hexcol, mix, darker

LAYOUT = [('idle', 4), ('run', 4), ('attack', 3), ('die', 4), ('zap', 3)]


def frames(draw):
	out = []
	for mode, n in LAYOUT:
		for i in range(n):
			out.append(draw({'mode': mode, 'step': i}))
	return out


def ramp(*hexes):
	return [hexcol(h) for h in hexes]


def fade(c, t, to=(0, 0, 0, 0)):
	"""dims and thins out a canvas for death frames (t 0..1)"""
	out = Canvas(c.w, c.h)
	for y in range(c.h):
		for x in range(c.w):
			p = c.get(x, y)
			if p is None:
				continue
			if t > 0.5 and (x * 7 + y * 13) % 5 < (t - 0.5) * 10:
				continue
			out.set(x, y, mix(p, darker(p, 0.4), t))
	return out


def flash(c, col, t):
	return c.recolor(lambda p: mix(p, col, t))


def squash(c, sy, base=None):
	"""vertical squash towards the bottom row that has pixels"""
	base = base if base is not None else max(y for y in range(c.h) for x in range(c.w) if c.get(x, y) is not None)
	out = Canvas(c.w, c.h)
	for y in range(c.h):
		for x in range(c.w):
			p = c.get(x, y)
			if p is not None:
				ny = base - (base - y) * sy
				out.set(x, ny, p)
	return out


def legs(c, xs, top, length, col, phase, spread=1):
	"""simple 1px legs; phase moves alternate legs forward and back"""
	for i, x in enumerate(xs):
		off = 0
		if phase is not None:
			off = spread if (i + phase) % 2 == 0 else -spread
		for j in range(length):
			c.set(x + (off * j) / max(1, length - 1), top + j, col)


def particles(c, pts, col):
	for x, y in pts:
		c.set(x, y, col)


def body_frame(w, h, rows, pal, legs_rows=None, ox=0, oy=0, legs_oy=None, extra=None):
	"""draws a hand-made body grid (rows) and optional leg rows below it"""
	c = Canvas(w, h)
	if legs_rows:
		c.grid(legs_rows, pal, ox, (legs_oy if legs_oy is not None else oy + len(rows)))
	c.grid(rows, pal, ox, oy)
	if extra:
		extra(c)
	return c


def replace_rows(rows, changes):
	"""changes: {row index: new row string}"""
	out = list(rows)
	for i, r in changes.items():
		out[i] = r
	return out


def bottom(c):
	return max(y for y in range(c.h) for x in range(c.w) if c.get(x, y) is not None)
