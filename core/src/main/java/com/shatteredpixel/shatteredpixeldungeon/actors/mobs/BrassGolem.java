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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GolemSprite;
import com.watabou.utils.Random;

//a heavy dwarven construct whose crushing blows cripple
public class BrassGolem extends Mob {

	{
		spriteClass = BrassSprite.class;
		HP = HT = 90;
		defenseSkill = 14;
		baseSpeed = 0.75f;
		EXP = 13;
		maxLvl = 23;
		properties.add(Property.INORGANIC);
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(10, 20); }

	@Override
	public int attackSkill( Char target ) { return 26; }

	@Override
	public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 8); }

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		if (Random.Int(3) == 0) Buff.prolong(enemy, Cripple.class, 3f);
		return damage;
	}

	public static class BrassSprite extends GolemSprite {
		@Override
		public void resetColor() { super.resetColor(); tint(0.9f, 0.6f, 0.2f, 0.45f); }
		@Override
		public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
