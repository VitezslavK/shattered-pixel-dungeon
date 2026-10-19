"""Points each monster's sprite class at its new sheet. Safe to run again."""
from wire import wire

M = 'actors/mobs/'
S = 'sprites/mod/'
wire(M + 'SewerLeech.java', 'LeechSprite', S + 'sewer_leech.png', 16, 12, idle_fps=4, run_fps=8)
wire(M + 'RatMatriarch.java', 'MatriarchSprite', S + 'rat_matriarch.png', 24, 17, idle_fps=2, run_fps=10)
wire(M + 'RatMatriarch.java', 'BroodSprite', S + 'rat_brood.png', 13, 10, idle_fps=3, run_fps=12)
wire(M + 'PrisonHound.java', 'HoundSprite', S + 'prison_hound.png', 18, 14, idle_fps=3, run_fps=14)
wire(M + 'Jailer.java', 'JailerSprite', S + 'jailer.png', 20, 22, idle_fps=2, run_fps=10)
wire(M + 'CrystalCrawler.java', 'CrawlerSprite', S + 'crystal_crawler.png', 16, 13, idle_fps=3, run_fps=12)
wire(M + 'CrystalQueen.java', 'QueenSprite', S + 'crystal_queen.png', 20, 22, idle_fps=4, run_fps=8)
wire(M + 'BrassGolem.java', 'BrassSprite', S + 'brass_golem.png', 18, 20, idle_fps=3, run_fps=10)
wire(M + 'GolemArchmage.java', 'ArchmageSprite', S + 'golem_archmage.png', 20, 22, idle_fps=3, run_fps=8)
wire(M + 'LightEater.java', 'EaterSprite', S + 'light_eater.png', 16, 16, idle_fps=3, run_fps=8)
wire(M + 'MirrorBeast.java', 'MirrorSprite', S + 'mirror_beast.png', 18, 14, idle_fps=3, run_fps=14)
wire(M + 'TreasureGoblin.java', 'GoblinSprite', S + 'treasure_goblin.png', 14, 15, idle_fps=3, run_fps=14, base='MobSprite')
wire(M + 'Shade.java', 'ShadeSprite', S + 'shade.png', 14, 16, idle_fps=4, run_fps=8)
wire(M + 'VoidWalker.java', 'VoidSprite', S + 'void_walker.png', 12, 17, idle_fps=4, run_fps=10)
wire(M + 'VoidHerald.java', 'HeraldSprite', S + 'void_herald.png', 22, 24, idle_fps=4, run_fps=8)
wire(M + 'FallenAngel.java', 'AngelSprite', S + 'fallen_angel.png', 18, 17, idle_fps=4, run_fps=8, base='MobSprite')
wire(M + 'Archlich.java', 'LichSprite', S + 'archlich.png', 18, 22, idle_fps=3, run_fps=8)
wire('actors/hero/necromancer/SkeletalServant.java', 'ServantSprite', S + 'skeletal_servant.png', 12, 15, idle_fps=3, run_fps=14)
wire('actors/hero/necromancer/SkeletalServant.java', 'RisenSprite', S + 'risen.png', 12, 15, idle_fps=3, run_fps=12)
wire('actors/hero/alchemist/Homunculus.java', 'HomunculusSprite', S + 'homunculus.png', 13, 12, idle_fps=3, run_fps=12)
print('wired')
