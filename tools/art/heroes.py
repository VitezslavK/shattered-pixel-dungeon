"""Hero sheets for the mod's classes: 21 frames of 12x15 per row, one row per armor tier (0-6).

Frame order (see HeroSprite): 0-1 idle, 2-7 run, 8-12 die, 13-15 attack, 16-17 operate, 18 fly,
19-20 read. Each hero is assembled from parts (head, torso, arms, legs) drawn as text grids; the
cloth letters c/C/d/a are recoloured per armor tier, so every tier keeps the hero's own shapes.
"""
from pix import Canvas, hexcol, mix

FW, FH = 12, 15

# ---------------------------------------------------------------- shared parts

# legs, 6 wide x 3 tall, placed under the torso. p/P = trousers light/dark, o = boots
LEGS = {
	'stand': [".pP.P.",
	          ".pP.P.",
	          ".oo.oo"],
	'run1':  [".pPP..",
	          ".p..P.",
	          "oo..oo"],
	'run2':  ["..pP..",
	          "..pP..",
	          "..oo.."],
	'run3':  ["..Pp..",
	          ".P..p.",
	          "oo..oo"],
	'run4':  [".PPp..",
	          "P...p.",
	          "o....o"],
	'run5':  ["..Pp..",
	          "..Pp..",
	          "..oo.."],
	'run6':  [".pP...",
	          ".p.PP.",
	          "oo..oo"],
	'fly':   [".pP.P.",
	          "..pP..",
	          "..oo.."],
}


# arms: grids placed relative to the shoulder. s = skin (hand), c/C cloth sleeve
ARMS = {
	'down':  ["c", "C", "s"],
	'fwd':   ["cc", "CCs"],
	'up':    ["s", "C", "c"],
	'back':  ["c", "C", "s"],
	'hold':  ["c", "Cs"],
}

# frame specs: (head dy, torso dy, legs, front arm, back arm)
POSES = [
	(0, 0, 'stand', 'down', 'back'),  # 0 idle
	(1, 1, 'stand', 'down', 'back'),  # 1 idle breathe
	(0, 0, 'run1', 'fwd', 'back'),  # 2-7 run
	(1, 1, 'run2', 'down', 'back'),
	(0, 0, 'run3', 'back2', 'fwd'),
	(0, 0, 'run4', 'back2', 'fwd'),
	(1, 1, 'run5', 'down', 'back'),
	(0, 0, 'run6', 'fwd', 'back'),
	None, None, None, None, None,  # 8-12 die, drawn per hero
	(0, 0, 'stand', 'up', 'back'),  # 13-15 attack
	(0, 0, 'run1', 'fwd', 'back'),
	(1, 0, 'run1', 'fwd', 'back'),
	(1, 1, 'stand', 'hold', 'back'),  # 16-17 operate
	(1, 1, 'stand', 'fwd', 'back'),
	(0, 0, 'fly', 'down', 'back'),  # 18 fly
	(0, 0, 'stand', 'hold', 'back'),  # 19-20 read
	(1, 0, 'stand', 'hold', 'back'),
]
ARMS['back2'] = ARMS['back']


class Hero:
	"""head: 8x7 grid, torso: 6x5 grid, pal: fixed colours, tiers: 7 dicts for c/C/d/a/p/P/o"""
	name = ''
	head = []
	torso = []
	pal = {}
	tiers = []
	held = None  # optional grid drawn in the hand for read/operate frames

	def palette(self, tier):
		p = dict(BASE_PAL)
		p.update(self.pal)
		p.update(self.tiers[tier])
		return p

	def compose(self, pose, tier):
		hdy, tdy, legs, front, back = pose
		pal = self.palette(tier)
		c = Canvas(FW, FH)
		# back arm, behind the torso
		c.grid(ARMS[back], pal, 2, 8 + tdy)
		c.grid(LEGS[legs], pal, 3, 12)
		c.grid(self.torso, pal, 3, 7 + tdy)
		c.grid(self.head, pal, 2, hdy)
		# front arm over the torso
		c.grid(ARMS[front], pal, 9, 8 + tdy - (1 if front == 'up' else 0))
		c.outline(factor=0.42)
		self.extras(c, pose, pal, tier)
		return c

	def extras(self, c, pose, pal, tier):
		pass

	def death(self, step, tier):
		"""5 frames, 8-12"""
		raise NotImplementedError

	def row(self, tier):
		frames = []
		for i, pose in enumerate(POSES):
			if pose is None:
				frames.append(self.death(i - 8, tier))
			else:
				frames.append(self.compose(pose, tier))
		return frames

	def sheet(self):
		return [self.row(t) for t in range(7)]


BASE_PAL = {
	'k': hexcol('#141218'),
	'w': hexcol('#f2efe6'),
	'e': hexcol('#101010'),
}


def tier_pal(c, C, d, a, p, P, o):
	return {'c': hexcol(c), 'C': hexcol(C), 'd': hexcol(d), 'a': hexcol(a),
	        'p': hexcol(p), 'P': hexcol(P), 'o': hexcol(o)}


# armor tiers shared by all heroes: 1 cloth, 2 leather, 3 mail, 4 scale, 5 plate. 0 and 6 are per hero
COMMON_TIERS = [
	None,
	tier_pal('#e7e0cf', '#b9ae96', '#857a66', '#5b7fb8', '#9a8f7a', '#6e6555', '#4a3b2c'),
	tier_pal('#a8743f', '#7c5228', '#55351a', '#d9b25a', '#6b4a2c', '#4a321d', '#2e2015'),
	tier_pal('#b9bfc9', '#8a909c', '#5f6470', '#d8dde6', '#5f6470', '#44474f', '#2b2d33'),
	tier_pal('#c7b06a', '#968148', '#66572e', '#5aa36b', '#6e6550', '#4d4636', '#2f2a20'),
	tier_pal('#e3e8ef', '#a9b3c2', '#6f7a8c', '#4f7fd6', '#8a93a3', '#5c6475', '#353a45'),
	None,
]


# ---------------------------------------------------------------- Necromancer

class Necromancer(Hero):
	"""deep violet hood with a bone clasp, ash-grey face, green soul-lit eyes"""
	name = 'necromancer'
	head = [
		"..hhhh..",
		".hhhhhh.",
		"hhhHHHHh",
		"hhHSssss",
		"hhHSgsgs",
		"hHHSssss",
		".HHHSss.",
	]
	torso = [
		"dcbcCd",
		"dCaCCd",
		"dCcCCd",
		"ddCCdd",
		"dd..dd",
	]
	pal = {
		'h': hexcol('#5a3a80'), 'H': hexcol('#311d4a'),
		's': hexcol('#c9cfc6'), 'S': hexcol('#8f978f'), 'g': hexcol('#5dff9a'),
		'b': hexcol('#e9e1c8'),
	}
	tiers = list(COMMON_TIERS)
	tiers[0] = tier_pal('#62557c', '#4a3f62', '#332b47', '#7dffad', '#3f3554', '#2b2439', '#1a1522')
	tiers[6] = tier_pal('#e6dfc8', '#b5ab90', '#2a2233', '#5dff9a', '#2a2233', '#1b1522', '#0f0c14')

	def extras(self, c, pose, pal, tier):
		# a faint soul wisp floats by the hand when reading or operating
		if pose[3] == 'hold':
			c.set(11, 7 + pose[1], hexcol('#5dff9a', 200))
			c.set(11, 6 + pose[1], hexcol('#b8ffd4', 140))

	def death(self, step, tier):
		# the body collapses into the robe, a skull remains and the soul drifts up
		pal = self.palette(tier)
		c = Canvas(FW, FH)
		if step == 0:
			c = self.compose((1, 1, 'stand', 'down', 'back'), tier)
		elif step == 1:
			c.grid(self.head, pal, 2, 4)
			c.grid(self.torso[:2] + self.torso[3:4], pal, 3, 11)
			c.outline(factor=0.42)
		elif step >= 2:
			c.grid(["..kkkk..", ".kddddk.", "kdCCCCdk", "kkkkkkkk"], pal, 1, 11)
			c.grid([".kkk.", "kwwwk", "kekek", ".kwk."], pal, 5 + (0 if step < 4 else 0), 8 + min(step, 3) - 1)
		if step >= 2:
			# soul rising
			for i in range(step - 1):
				c.set(6 - i % 2, 7 - (step - 2) * 2 - i * 2, hexcol('#5dff9a', 220 - i * 50))
		return c


class Arcanist(Hero):
	"""navy hair, a gold circlet with a cyan gem, a long rune-marked coat, an orb that circles the hand"""
	name = 'arcanist'
	head = [
		"..hhhh..",
		".hhhhhhh",
		"hhhhhhhh",
		"hHyyyyyQ",
		"hHhsesse",
		"hHhsssss",
		".HhhSss.",
	]
	torso = [
		"dcCcad",
		"dCaaCd",
		"dcCaad",
		"dCCCdd",
		"dd..dd",
	]
	pal = {
		'h': hexcol('#2f3f78'), 'H': hexcol('#1c2650'),
		's': hexcol('#f0c9a0'), 'S': hexcol('#c99a74'),
		'y': hexcol('#e6c25a'), 'Q': hexcol('#5ef2ff'),
	}
	tiers = list(COMMON_TIERS)
	tiers[0] = tier_pal('#4a6fbf', '#34529a', '#22386e', '#e6c25a', '#3a3f55', '#272a3a', '#1a1b24')
	tiers[6] = tier_pal('#33397a', '#262b5e', '#181b40', '#ffd76a', '#262b5e', '#181b40', '#0e1028')
	ORB = hexcol('#5ef2ff')
	ORB_GLOW = hexcol('#c8fbff', 170)

	def extras(self, c, pose, pal, tier):
		# the arcane orb bobs beside the hero
		if pose[3] in ('down', 'hold', 'fwd'):
			y = 6 + pose[0]
			c.set(11, y, self.ORB)
			c.set(11, y - 1, self.ORB_GLOW)

	def death(self, step, tier):
		# the body flickers out into arcane sparks, leaving the coat and the circlet
		pal = self.palette(tier)
		if step == 0:
			c = self.compose((1, 1, 'stand', 'down', 'back'), tier)
			return c.recolor(lambda p: mix(p, self.ORB, 0.35))
		c = Canvas(FW, FH)
		if step == 1:
			c = self.compose((1, 1, 'stand', 'down', 'back'), tier).recolor(lambda p: mix(p, self.ORB_GLOW, 0.7))
			return c
		c.grid(["..kkkkk..", ".kdCCCdk.", "kdcCcCcdk", "kkkkkkkkk"], pal, 1, 11)
		c.grid(["kyyQk"], pal, 6, 10)
		for i, (x, y) in enumerate([(3, 8), (8, 6), (5, 4), (9, 3), (2, 5), (6, 1)]):
			if i < (step - 1) * 2:
				c.set(x + (step % 2), y - (step - 2), mix(self.ORB, self.ORB_GLOW, (i % 2) * 0.6))
		return c


class Alchemist(Hero):
	"""messy copper hair, brass goggles, a leather apron with a strap of coloured vials"""
	name = 'alchemist'
	head = [
		"h.hh.h..",
		"hhhhhhh.",
		"hhhhhhhh",
		"hHGGgGGg",
		"hHhsesse",
		"hHhsssss",
		".Hh.Sss.",
	]
	torso = [
		"drvuCd",
		"dcCccd",
		"dcacCd",
		"dCCCdd",
		"dd..dd",
	]
	pal = {
		'h': hexcol('#d0702e'), 'H': hexcol('#8e4419'),
		's': hexcol('#f2c79c'), 'S': hexcol('#c4946b'),
		'G': hexcol('#b88a3a'), 'g': hexcol('#8ff0e0'),
		'r': hexcol('#e0473c'), 'v': hexcol('#55d46b'), 'u': hexcol('#4f8ff0'),
	}
	tiers = list(COMMON_TIERS)
	tiers[0] = tier_pal('#9c7a4e', '#78593a', '#523c26', '#d08a3c', '#5a4a3a', '#3f3328', '#2a211a')
	tiers[6] = tier_pal('#e0ad5a', '#ad7f36', '#704f20', '#6fe08a', '#5c4426', '#3e2d18', '#24190e')
	FLASK = hexcol('#6fe08a')
	BUBBLE = hexcol('#d8ffe0', 200)

	def extras(self, c, pose, pal, tier):
		# a bubbling flask in hand while reading or working
		if pose[3] == 'hold':
			y = 8 + pose[1]
			c.set(11, y, self.FLASK)
			c.set(11, y - 1, pal['k'])
			c.set(11, y - 2 - pose[0], self.BUBBLE)

	def death(self, step, tier):
		# falls over and the flask shatters into a green puddle
		pal = self.palette(tier)
		if step == 0:
			return self.compose((1, 1, 'stand', 'fwd', 'back'), tier)
		c = Canvas(FW, FH)
		if step == 1:
			c.grid(self.head, pal, 2, 4)
			c.grid(self.torso[:2] + self.torso[3:4], pal, 3, 11)
			c.outline(factor=0.42)
			return c
		c.grid(["...kkkkkk..", ".kkhhdcCck.", "khGgsdrvuck", "kkkkkkkkkkk"], pal, 0, 11)
		splash = [(9, 10), (10, 9), (11, 10), (8, 9), (10, 11), (11, 11)]
		for i, (x, y) in enumerate(splash[:2 + (step - 2) * 2]):
			c.set(x, y + (step - 2), mix(self.FLASK, self.BUBBLE, (i % 2) * 0.5))
		return c


def build(cls, path):
	from pix import Canvas as _C
	from PIL import Image
	h = cls()
	rows = h.sheet()
	im = Image.new('RGBA', (256, 128), (0, 0, 0, 0))
	for t, row in enumerate(rows):
		for i, f in enumerate(row):
			im.paste(f.image(), (i * FW, t * FH))
	im.save(path)
	return im


def avatar(cls):
	"""24x32 portrait for the ending scene: the idle frame drawn at double size with
	the outline redone at 1px so it stays crisp"""
	h = cls()
	pal = h.palette(0)
	small = Canvas(FW, FH)
	small.grid(ARMS['back'], pal, 2, 8)
	small.grid(LEGS['stand'], pal, 3, 12)
	small.grid(h.torso, pal, 3, 7)
	small.grid(h.head, pal, 2, 0)
	small.grid(ARMS['down'], pal, 9, 8)
	big = Canvas(24, 32)
	for y in range(FH):
		for x in range(FW):
			p = small.get(x, y)
			if p is None:
				continue
			for dy in (0, 1):
				for dx in (0, 1):
					big.set(x * 2 + dx, y * 2 + dy + 1, p)
	# soften: highlight the top-left pixel of each colour block, shade the bottom-right one
	out = big.copy()
	for y in range(32):
		for x in range(24):
			p = big.get(x, y)
			if p is None or p == pal.get('e'):
				continue
			up, left = big.get(x, y - 1), big.get(x - 1, y)
			down, right = big.get(x, y + 1), big.get(x + 1, y)
			if up is not None and up != p and left is not None and left != p:
				out.set(x, y, mix(p, (255, 255, 255, 255), 0.12))
			elif down is not None and down != p and right is not None and right != p:
				out.set(x, y, mix(p, (0, 0, 0, 255), 0.18))
	out.outline(factor=0.42)
	h.extras(out, (0, 0, 'stand', 'none', 'back'), pal, 0)
	return out
