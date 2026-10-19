"""Tilesets for the mod's floors: ice caves (from the caves), burned city (from the city), abyss (from the halls).

The tile shapes have to stay where the game expects them (wall joins, raised walls, doors), so each
tileset takes its shapes from the original region's sheet and paints every surface with a new
material: brightness picks the shade, a per-tile pattern adds the material's own texture, and
coloured details (moss, crystals, gems) turn into the region's accent."""
import colorsys
import math
from PIL import Image


def hexc(h):
	h = h.lstrip('#')
	return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lerp(a, b, t):
	return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def ramp_at(ramp, v):
	v = max(0.0, min(0.999, v))
	f = v * (len(ramp) - 1)
	i = int(f)
	return ramp[i] if f - i < 0.5 else ramp[i + 1]


def h2(x, y, s=0):
	n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xffffffff
	n = (n ^ (n >> 13)) * 1274126177 & 0xffffffff
	return ((n ^ (n >> 16)) & 0xffff) / 65535.0


def vnoise(x, y, scale, seed):
	"""smooth value noise"""
	fx, fy = x / scale, y / scale
	x0, y0 = int(math.floor(fx)), int(math.floor(fy))
	tx, ty = fx - x0, fy - y0
	tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
	a, b = h2(x0, y0, seed), h2(x0 + 1, y0, seed)
	c, d = h2(x0, y0 + 1, seed), h2(x0 + 1, y0 + 1, seed)
	return (a + (b - a) * tx) * (1 - ty) + (c + (d - c) * tx) * ty


def lum(c):
	return (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255.0


MATERIALS = {
	'ice': {
		'ramp': [hexc(h) for h in ('#081422', '#16304a', '#2a5476', '#4a82a8', '#7ab4d4', '#b8e2f4', '#f0fcff')],
		'accent': [hexc(h) for h in ('#1a6a8a', '#3ac8e8', '#a8f4ff')],
	},
	'burned': {
		'ramp': [hexc(h) for h in ('#0c0806', '#1e1410', '#33241c', '#4a362a', '#6a5040', '#8e7058', '#b8987a')],
		'accent': [hexc(h) for h in ('#8a2a0a', '#e8641a', '#ffc040')],
	},
	'abyss': {
		'ramp': [hexc(h) for h in ('#06040c', '#120c22', '#20163a', '#30224e', '#463468', '#62508a', '#8e7ab4')],
		'accent': [hexc(h) for h in ('#4a1a7a', '#a050f0', '#f0c8ff')],
	},
}


def pattern(kind, x, y, v, edge):
	"""returns (brightness offset, special colour or None)"""
	tx, ty = x % 16, y % 16
	tile = (x // 16, y // 16)
	n = h2(x, y, 1) - 0.5
	if kind == 'ice':
		off = (vnoise(x, y, 5, 1) - 0.5) * 0.14
		# long diagonal glints across frozen surfaces
		if v > 0.4 and (tx - ty + tile[0] * 5) % 13 == 0 and vnoise(x, y, 4, 8) > 0.5:
			off += 0.14
		# hairline cracks along noise contours
		if v > 0.3 and abs(vnoise(x, y, 6, 3) - 0.5) < 0.025:
			off -= 0.16
		# frost specks
		if v > 0.25 and h2(x, y, 7) < 0.008:
			return off, MATERIALS['ice']['ramp'][-1]
		return off, None
	if kind == 'burned':
		off = (vnoise(x, y, 4, 2) - 0.5) * 0.16 - 0.03
		# soot blotches
		if vnoise(x, y, 6, 3) < 0.3:
			off -= 0.08
		# embers glowing in the dark cracks
		if v < 0.3 and not edge and h2(x, y, 5) < 0.025:
			acc = MATERIALS['burned']['accent']
			return off, acc[1] if h2(x, y, 9) < 0.7 else acc[2]
		return off, None
	if kind == 'abyss':
		off = (vnoise(x, y, 5, 4) - 0.5) * 0.12
		# violet veins along noise contours
		if v > 0.25 and abs(vnoise(x, y, 7, 5) - 0.5) < 0.03:
			return off, MATERIALS['abyss']['accent'][0 if v < 0.5 else 1]
		# stars in the stone
		if h2(x, y, 11) < 0.007:
			return off, MATERIALS['abyss']['accent'][2] if h2(x, y, 4) < 0.5 else (255, 255, 255)
		return off, None
	return 0, None


def retexture(src_path, kind, dst_path):
	src = Image.open(src_path).convert('RGBA')
	w, h = src.size
	out = Image.new('RGBA', (w, h), (0, 0, 0, 0))
	mat = MATERIALS[kind]
	sp, op = src.load(), out.load()
	for y in range(h):
		for x in range(w):
			r, g, b, a = sp[x, y]
			if a == 0:
				continue
			v = lum((r, g, b))
			hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
			# outline / deep shadow pixels stay near black so the shapes keep reading
			edge = v < 0.08
			if edge:
				op[x, y] = mat['ramp'][0] + (a,)
				continue
			if ss > (0.6 if kind == 'burned' else 0.4) and 0.1 < ll < 0.9:
				# a coloured detail in the original (moss, gems, banners): use the accent
				col = ramp_at(mat['accent'], ll * (0.8 if kind == 'abyss' else 1.2))
				op[x, y] = col + (a,)
				continue
			off, special = pattern(kind, x, y, v, edge)
			col = special if special else ramp_at(mat['ramp'], v * 1.1 + off)
			op[x, y] = col + (a,)
	out.save(dst_path)
	return out


def water(kind, dst_path):
	"""32x32 seamless water"""
	im = Image.new('RGBA', (32, 32))
	px = im.load()
	for y in range(32):
		for x in range(32):
			wave = math.sin((x + y * 0.5) * math.pi / 8) * 0.5 + math.sin((x * 0.5 - y) * math.pi / 16) * 0.5
			n = h2(x, y, 2)
			if kind == 'ice':
				base = lerp(hexc('#1a4a6a'), hexc('#3a8ab0'), 0.5 + wave * 0.3)
				# floating ice plates
				if (math.sin(x * math.pi / 16) * math.cos(y * math.pi / 16)) > 0.55:
					base = lerp(hexc('#a8d8ec'), hexc('#e8f8ff'), n)
				elif n < 0.04:
					base = hexc('#e8f8ff')
			elif kind == 'burned':
				base = lerp(hexc('#1c1410'), hexc('#3a2a20'), 0.5 + wave * 0.35)
				if wave > 0.85 and n < 0.4:
					base = hexc('#e8641a')  # embers reflected on the ashy surface
				elif n < 0.03:
					base = hexc('#6a5040')
			else:
				base = lerp(hexc('#0a0616'), hexc('#2a1a4a'), 0.5 + wave * 0.35)
				if n < 0.025:
					base = hexc('#f0c8ff') if n < 0.012 else hexc('#ffffff')
				elif wave > 0.9:
					base = hexc('#6a3aa8')
			px[x, y] = base + (255,)
	im.save(dst_path)
	return im
