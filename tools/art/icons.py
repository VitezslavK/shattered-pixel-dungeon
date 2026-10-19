"""The mod's 16x16 hero icons (subclasses, armor abilities, evolutions) for interfaces/hero_icons.png.

Subclass and ability icons sit on a coloured tile like the original ones; evolution icons are a
gold symbol on a dark tile with a gold frame, so an evolved subclass is recognisable at a glance."""
from pix import Canvas, hexcol, mix

PAL = {
	'k': hexcol('#15131a'), 'w': hexcol('#ffffff'), 'W': hexcol('#c9ccd4'),
	'b': hexcol('#ece5cf'), 'B': hexcol('#b3a98e'),
	'g': hexcol('#5dff9a'), 'G': hexcol('#23874c'),
	'y': hexcol('#ffd76a'), 'Y': hexcol('#b8862a'),
	'r': hexcol('#e8473c'), 'R': hexcol('#8a1f1a'),
	'o': hexcol('#f6a33a'), 'O': hexcol('#b35f16'),
	'c': hexcol('#6af3ff'), 'C': hexcol('#3478d0'),
	'p': hexcol('#b878f0'), 'P': hexcol('#5c2f8f'),
	's': hexcol('#d3d8e0'), 'S': hexcol('#7b818e'),
	'n': hexcol('#9a6634'), 'N': hexcol('#5c3b1d'),
	'l': hexcol('#6bd04f'), 'L': hexcol('#2f7a2a'),
	'm': hexcol('#ff9ad0'), 'M': hexcol('#b04f88'), 'e': hexcol('#101010'),
}


def tile(bg, frame=None):
	"""rounded tile, lighter towards the top"""
	c = Canvas(16, 16)
	top = mix(bg, (255, 255, 255, 255), 0.18)
	for y in range(16):
		for x in range(16):
			if (x in (0, 15) and y in (0, 15)):
				continue
			c.set(x, y, mix(top, bg, y / 15))
	if frame:
		for i in range(1, 15):
			for x, y in ((i, 0), (i, 15), (0, i), (15, i)):
				c.set(x, y, frame)
		c.set(1, 1, frame); c.set(14, 1, frame); c.set(1, 14, frame); c.set(14, 14, frame)
	return c


def icon(grid, bg, frame=None, pal=None):
	c = tile(bg, frame)
	p = dict(PAL)
	if pal:
		p.update(pal)
	c.grid(grid, p)
	return c


EVO_BG = hexcol('#141a3a')
EVO_FRAME = hexcol('#d9a73a')


def evo(grid):
	# evolution symbols are drawn in gold whatever letters they use for metal
	return icon(grid, EVO_BG, EVO_FRAME)


SKULL = [
	"....kkkkkk....",
	"...kbbbbbbk...",
	"..kbbbbbbbbk..",
	"..kbkkbbkkbk..",
	"..kbkkbbkkbk..",
	"..kbbbkkbbbk..",
	"...kbbbbbbk...",
	"....kbkbkk....",
	"....kkkkk.....",
]


def pad(rows, top=0, left=0, h=16, w=16):
	out = ["." * w for _ in range(top)]
	for r in rows:
		out.append(("." * left + r).ljust(w, '.')[:w])
	while len(out) < h:
		out.append("." * w)
	return out


ICONS = {}

# ------------------------------------------------ necromancer
ICONS['BONELORD'] = icon(pad([
	"..k.k..k..k.k..",
	"..kbkbkbkbkbk..",
	"..kyyyyyyyyyk..",
] + SKULL[1:], 2, 0), hexcol('#5c3a8a'))

ICONS['SOUL_REAPER'] = icon([
	"................",
	"....kkkkkkk.....",
	"..kksssssssk....",
	".ksSkkkkkkssk...",
	".kSk......kssk..",
	"..k........kSk..",
	"............kk..",
	"......kgk..kNk..",
	".....kgwgk.kNk..",
	".....kgggkkNk...",
	"......kgk.kNk...",
	"..........kNk...",
	".........kNk....",
	".........kNk....",
	"........kNk.....",
	"................",
], hexcol('#1f5a4a'))

ICONS['ARMY_OF_THE_DEAD'] = icon([
	"................",
	"................",
	"...kkkk..kkkk...",
	"..kbbbbk.kbbbbk.",
	"..kbkbkk.kbkbkk.",
	"..kbbbbk.kbbbbk.",
	"...kbkk...kbkk..",
	"....kkkkkk......",
	"...kbbbbbbk.....",
	"...kbkkbkkbk....",
	"...kbkkbkkbk....",
	"...kbbbkbbbk....",
	"....kbbbbbk.....",
	".....kbkbk......",
	".....kkkkk......",
	"................",
], hexcol('#2f5a2a'))

ICONS['CORPSE_EXPLOSION'] = icon([
	"................",
	"..y....o....y...",
	"...y...o...y....",
	"....kkkkkkk.....",
	"oo.kbbbbbbbk.oo.",
	"..kbkkbbbkkbk...",
	"..kbkkbbbkkbk...",
	"..kbbbbkbbbbk...",
	"...kbbbbbbbk....",
	"....kbkbkbk.....",
	"...y.kkkkk.y....",
	"..y...rrr...y...",
	".y...rorro...y..",
	".....rooor......",
	"......rrr.......",
	"................",
], hexcol('#8a3a1a'))

ICONS['SOUL_SWAP'] = icon([
	"................",
	"....kkkk........",
	"...kgggk........",
	"..kgk..kkk......",
	"..kgk..kgk......",
	".kggg.kgggk.....",
	"..kgk..kgk......",
	"......kkkk......",
	"......kkkk......",
	"......kpk..kpk..",
	".....kpppk.ppp..",
	"......kpk..kpk..",
	"......kkk..kpk..",
	"........kpppk...",
	"........kkkk....",
	"................",
], hexcol('#3a2a5a'))

# ------------------------------------------------ arcanist
ICONS['RUNESMITH'] = icon([
	"................",
	"..kkkkkkk.......",
	"..kSsssSk.......",
	"..kSsssSk.......",
	"..kkkkkkk.......",
	"....kNk.........",
	"....kNk..kkkkk..",
	"....kNk.kSSSSSk.",
	"....kNk.kScScSk.",
	"....kNk.kSScSSk.",
	"....kNk.kScScSk.",
	"....kNk.kSSSSSk.",
	"....kkk..kkkkk..",
	"................",
	"................",
	"................",
], hexcol('#2a4a8a'))

ICONS['RELIC_KEEPER'] = icon([
	"................",
	".....kkkkkk.....",
	"....kyk..kyk....",
	"....kyk..kyk....",
	".....kykkyk.....",
	"......kyyk......",
	".....kkyykk.....",
	"....kycccCyk....",
	"...kycwcccCyk...",
	"...kycccccCyk...",
	"...kyCcccCCyk...",
	"....kyCCCCyk....",
	".....kyyyyk.....",
	"......kkkk......",
	"................",
	"................",
], hexcol('#1f5a6a'))

ICONS['ARCANE_DISCHARGE'] = icon([
	"................",
	".......c........",
	"...c...c...c....",
	"....c..c..c.....",
	".....cwwwc......",
	"..cccwwwwwccc...",
	".....cwwwc......",
	"....c..c..c.....",
	"...c...c...c....",
	".......c........",
	"..........c.....",
	"...........cc...",
	"............c...",
	"...c.......c....",
	"..cc............",
	"................",
], hexcol('#2a2a6a'))

ICONS['ARTIFACT_OVERLOAD'] = icon([
	"................",
	"...y.......y....",
	"....y.kkk.y.....",
	".....kpppk......",
	"....kpwppPk.....",
	"...kpwpkpPPk....",
	"..kpppkwkPPPk...",
	"..kpPPPkPPPPk...",
	"...kpPkPPPPk....",
	"....kPkPPPk.....",
	".....kPPPk......",
	"......kkk.......",
	"....y.....y.....",
	"...y.......y....",
	"................",
	"................",
], hexcol('#4a2a6a'))

ICONS['MIRROR_BARRIER'] = icon([
	"................",
	"......kkkk......",
	"....kkccwwkk....",
	"...kcccwwcckk...",
	"..kcccwwccccSk..",
	"..kccwwcccccSk..",
	"..kcwwcccccwSk..",
	"..kcwcccccwwSk..",
	"..kccccccwwcSk..",
	"..kcccccwwccSk..",
	"...kcccwwccSk...",
	"....kkcccSkk....",
	"......kkkk......",
	"................",
	"................",
	"................",
], hexcol('#3a6a9a'))

# ------------------------------------------------ alchemist
ICONS['BOMBARDIER'] = icon([
	"................",
	"..........y.....",
	"........yoy.....",
	".........o......",
	"........kN......",
	".......kNk......",
	".....kkkkkk.....",
	"....kSSSSSSk....",
	"...kSwSSSSSSk...",
	"...kSwSSSSSSk...",
	"...kSSSSSSSkk...",
	"...kSSSSSSSkk...",
	"....kSSSSSkk....",
	".....kkkkkk.....",
	"................",
	"................",
], hexcol('#9a5a1a'))

ICONS['TRANSMUTER'] = icon([
	"................",
	"......kkkk......",
	".......kk.......",
	"..k....kk....k..",
	".kyk..kllk..kyk.",
	"..kyk.kllk.kyk..",
	"...k.kllllk.k...",
	".....kllllk.....",
	"....kllwlllk....",
	"....klwlllLk....",
	"....kllllLLk....",
	".....kkkkkk.....",
	"..k.........k...",
	".kyk.......kyk..",
	"..kyyyyyyyyyk...",
	"................",
], hexcol('#2a5a3a'))

ICONS['ACID_RAIN'] = icon([
	"................",
	".....kkkkk......",
	"...kkWWWWWkk....",
	"..kWWWwwWWWWk...",
	".kWWWwwWWWWWWk..",
	".kSSSSSSSSSSSk..",
	"..kkkkkkkkkkk...",
	"....l...l...l...",
	"...l...l...l....",
	"......l...l...l.",
	".....l...l...l..",
	"..l...l.........",
	".l...l....l.....",
	"..........l.....",
	"...L...L...L....",
	"................",
], hexcol('#2a4a3a'))

ICONS['ELIXIR_OF_FURY'] = icon([
	"................",
	".....o..o.......",
	"....oro.oro.....",
	".....kkkk.......",
	"......bb........",
	"......kk........",
	".....krrk.......",
	"....krrrrk......",
	"...krwrrrRk.....",
	"...krwrrrRk.....",
	"...krrrrRRk.....",
	"...kRrrrRRk.....",
	"....kRRRRk......",
	".....kkkk.......",
	"................",
	"................",
], hexcol('#6a1a1a'))

ICONS['POLYMORPH'] = icon([
	"................",
	"................",
	"....kkk.kkk.....",
	"...kwwwkwwwk....",
	"..kwwwwwwwwwkk..",
	".kwwwwwwwwwwkSk.",
	".kwwwwwwwwwkSSek",
	".kwwwwwwwwwkSSSk",
	"..kwwwwwwwwwkkk.",
	"...kwwwwwwwk....",
	"....kSkkkSk.....",
	"....kSk.kSk.....",
	"....kkk.kkk.....",
	"................",
	"...m..m....m....",
	"................",
], hexcol('#8a4a7a'))

# ------------------------------------------------ evolutions, in subclass order
EVOLUTIONS = [
	('warlord', [  # berserker: crowned axe
		"................",
		"...y.y.y........",
		"...yyyyy........",
		"....kkkk..kkk...",
		"...kyyyYkkyyYk..",
		"..kyyyyYkyyyYk..",
		"..kyyyyYYyyyYk..",
		"...kyyykYyyYk...",
		"....kkkkYkkk....",
		"........Y.......",
		"........Y.......",
		"........Y.......",
		"........Y.......",
		"........Y.......",
		"................",
		"................"]),
	('blademaster', [  # gladiator: crossed swords
		"................",
		"..y.........y...",
		"...y.......y....",
		"....y.....y.....",
		".....y...y......",
		"......y.y.......",
		".......y........",
		"......y.y.......",
		".....y...y......",
		"...YYY...YYY....",
		"....Y.....Y.....",
		"...Y.......Y....",
		"..Y.........Y...",
		"................",
		"................",
		"................"]),
	('spellblade', [  # battlemage: blade with a star
		"................",
		"...........y....",
		"....y.....y.....",
		"...yyy...y......",
		"....y...y.......",
		".......y........",
		"......y.........",
		".....y..........",
		"..YYYy..........",
		"....Y...........",
		"...Y............",
		"..Y.............",
		"................",
		"................",
		"................",
		"................"]),
	('lich', [  # warlock: crowned skull
		"................",
		"...y..y..y......",
		"...yyyyyyy......",
		"...kkkkkkk......",
		"..kyyyyyyyk.....",
		"..kykkykkyk.....",
		"..kykgykgyk.....",
		"..kyyykyyyk.....",
		"...kyyyyyk......",
		"....kykyk.......",
		"....kkkkk.......",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('phantom', [  # assassin: hooded mask
		"................",
		"......yyy.......",
		".....yYYYy......",
		"....yYkkkYy.....",
		"...yYkyykkYy....",
		"...yYkkkykYy....",
		"...yYkkkkkYy....",
		"...yYYkkkYYy....",
		"....yYYYYYy.....",
		".....y.y.y......",
		"....y.y.y.......",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('windwalker', [  # freerunner: wind swirls
		"................",
		"..yyyyyyy.......",
		"........yy......",
		"..yyyy...y......",
		".....yy.y.......",
		"..........yyy...",
		".yyyyyyyyy...y..",
		"............y...",
		"...yyyyyyyyy....",
		"................",
		"..yyyyy.........",
		"......yy........",
		".......y........",
		"................",
		"................",
		"................"]),
	('deadeye', [  # sniper: eye in a crosshair
		"................",
		".......y........",
		".......y........",
		"....kkkkkkk.....",
		"...kyyyyyyyk....",
		"..kyykkkkkyyk...",
		"yykyk.kyk.kykyy.",
		"..kyykkkkkyyk...",
		"...kyyyyyyyk....",
		"....kkkkkkk.....",
		".......y........",
		".......y........",
		"................",
		"................",
		"................",
		"................"]),
	('archdruid', [  # warden: great tree
		"................",
		".....yyyyy......",
		"...yyyyyyyyy....",
		"..yyyYyyyYyyy...",
		"..yyYyyYyyYyy...",
		"...yyyyYyyyy....",
		".....yYYYy......",
		"......YYY.......",
		"......YYY.......",
		".....YYYYY......",
		"....Y.Y.Y.Y.....",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('warmaster', [  # champion: war banner
		"................",
		"...Y............",
		"...Yyyyyyyy.....",
		"...Yyykyyyyy....",
		"...Yykkkyyy.....",
		"...Yyykyyyyy....",
		"...Yyyyyyyy.....",
		"...Y............",
		"...Y............",
		"...Y............",
		"...Y............",
		"..YYY...........",
		"................",
		"................",
		"................",
		"................"]),
	('grandmaster', [  # monk: fist
		"................",
		"................",
		"....kkkkkkk.....",
		"...kyykyykyk....",
		"...kyykyykyyk...",
		"...kyykyykyyk...",
		"..kkkkkkkkyyk...",
		"..kyyyyyyyyyk...",
		"..kyyyyyyyyk....",
		"...kYyyyyyk.....",
		"....kYYYYk......",
		"....kYYYYk......",
		"....kkkkkk......",
		"................",
		"................",
		"................"]),
	('hierophant', [  # priest: radiant sun with an eye
		"................",
		".......y........",
		"...y...y...y....",
		"....y.kkk.y.....",
		".....kyyyk......",
		"..yykyykyyky....",
		".....kyyyk.yy...",
		"....y.kkk.y.....",
		"...y...y...y....",
		".......y........",
		"................",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('templar', [  # paladin: shield with a cross
		"................",
		"...kkkkkkkkk....",
		"...kyyyYyyyk....",
		"...kyyyYyyyk....",
		"...kYYYYYYYk....",
		"...kyyyYyyyk....",
		"...kyyyYyyyk....",
		"....kyyYyyk.....",
		".....kyYyk......",
		"......kkk.......",
		"................",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('bone king', [  # bonelord: skull on a throne of bones
		"................",
		"..y.y.y.y.y.....",
		"..yyyyyyyyy.....",
		"...kkkkkkk......",
		"..kyyyyyyyk.....",
		"..kykkykkyk.....",
		"..kyyykyyyk.....",
		"...kykykyk......",
		".YY.kkkkk.YY....",
		"..YYYYYYYYY.....",
		".YY.......YY....",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('harbinger', [  # soul reaper: hourglass of souls
		"................",
		"...YYYYYYYYY....",
		"....kyyyyyk.....",
		"....kyyyyyk.....",
		".....kyyyk......",
		"......kyk.......",
		"......kgk.......",
		".....kgggk......",
		"....kgggggk.....",
		"....kgggggk.....",
		"...YYYYYYYYY....",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('runemaster', [  # runesmith: standing stone with a big rune
		"................",
		".....kkkkk......",
		"....kYYYYYk.....",
		"...kYyYYYyYk....",
		"...kYYyYyYYk....",
		"...kYYYyYYYk....",
		"...kYYyYyYYk....",
		"...kYyYYYyYk....",
		"...kYYYYYYYk....",
		"..kkkkkkkkkkk...",
		"................",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('archivist', [  # relic keeper: open book with a star
		"................",
		".......y........",
		"......yyy.......",
		".......y........",
		"..kkkk...kkkk...",
		".kyyyykkkyyyyk..",
		".kyYYyykyyYYyk..",
		".kyyyyykyyyyyk..",
		".kyYYyykyyYYyk..",
		".kyyyyykyyyyyk..",
		"..kkkkkkkkkkk...",
		"................",
		"................",
		"................",
		"................",
		"................"]),
	('demolisher', [  # bombardier: big blast
		"................",
		"...y...y...y....",
		"....y..y..y.....",
		".....yyyyy......",
		"..y.yyYYYyy.y...",
		"...yyYkkkYyy....",
		"yyyyYkkkkkYyyyy.",
		"...yyYkkkYyy....",
		"..y.yyYYYyy.y...",
		".....yyyyy......",
		"....y..y..y.....",
		"...y...y...y....",
		"................",
		"................",
		"................",
		"................"]),
	('philosopher', [  # transmuter: the philosopher's stone in a circle
		"................",
		".....yyyyy......",
		"...yy.....yy....",
		"..y.........y...",
		"..y...kkk...y...",
		".y...krrrk...y..",
		".y..krwrrRk..y..",
		".y..krrrRRk..y..",
		".y...kRRRk...y..",
		"..y...kkk...y...",
		"..y.........y...",
		"...yy.....yy....",
		".....yyyyy......",
		"................",
		"................",
		"................"]),
]

# slot of each icon in hero_icons.png (index = row * 8 + column)
SLOTS = {
	'BONELORD': 12, 'SOUL_REAPER': 13,
	'ARMY_OF_THE_DEAD': 35, 'CORPSE_EXPLOSION': 36, 'SOUL_SWAP': 37,
	'RUNESMITH': 128, 'RELIC_KEEPER': 129, 'ARCANE_DISCHARGE': 130, 'ARTIFACT_OVERLOAD': 131,
	'MIRROR_BARRIER': 132, 'BOMBARDIER': 133, 'TRANSMUTER': 134, 'ACID_RAIN': 135,
	'ELIXIR_OF_FURY': 136, 'POLYMORPH': 137,
}
EVOLUTIONS_START, EVOLUTIONS_EXT = 112, 144


def all_icons():
	"""(slot, canvas) for every icon the mod owns"""
	out = [(SLOTS[n], c) for n, c in ICONS.items()]
	for i, (name, grid) in enumerate(EVOLUTIONS):
		slot = EVOLUTIONS_START + i if i < 14 else EVOLUTIONS_EXT + i - 14
		out.append((slot, evo(grid)))
	return out
