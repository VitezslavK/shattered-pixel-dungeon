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
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.watabou.utils.Random;

//a fast, lean hound kept by the prison guards
public class PrisonHound extends Mob {

	{
		spriteClass = HoundSprite.class;
		HP = HT = 22;
		defenseSkill = 9;
		baseSpeed = 1.5f;
		EXP = 6;
		maxLvl = 13;
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(2, 8); }

	@Override
	public int attackSkill( Char target ) { return 13; }

	@Override
	public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 2); }

	public static class HoundSprite extends RatSprite {
		public HoundSprite(){ super(); scale.set(1.25f); }
		@Override
		public void resetColor() { super.resetColor(); tint(0.25f, 0.2f, 0.15f, 0.55f); }
		@Override
		public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
