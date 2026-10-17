/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.watabou.utils.Random;

//abyss monster with a mirrored hide that reflects part of every melee blow
public class MirrorBeast extends Mob {

	{
		spriteClass = MirrorSprite.class;
		HP = HT = 160;
		defenseSkill = 20;
		EXP = 20;
		maxLvl = Hero.MAX_LEVEL - 1;
		properties.add(Property.INORGANIC);
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(22, 34); }

	@Override
	public int attackSkill( Char target ) { return 40; }

	@Override
	public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 20); }

	@Override
	public int defenseProc( Char enemy, int damage ) {
		damage = super.defenseProc(enemy, damage);
		if (enemy != null && damage > 0 && com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.adjacent(pos, enemy.pos)){
			enemy.damage(Math.max(1, damage/4), this);
		}
		return damage;
	}

	public static class MirrorSprite extends StatueSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(0.8f, 0.9f, 1f, 0.45f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}
}
