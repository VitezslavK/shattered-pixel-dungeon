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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WraithSprite;
import com.watabou.utils.Random;

//a demon halls spirit that stays invisible until it is right next to its prey
public class Shade extends Mob {

	{
		spriteClass = ShadeSprite.class;
		HP = HT = 70;
		defenseSkill = 28;
		EXP = 15;
		maxLvl = 30;
		flying = true;
		properties.add(Property.UNDEAD);
		properties.add(Property.DEMONIC);
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(18, 26); }

	@Override
	public int attackSkill( Char target ) { return 34; }

	@Override
	protected boolean act() {
		//hidden unless adjacent to its target
		boolean close = enemy != null && Dungeon.level.adjacent(pos, enemy.pos);
		invisible = close ? 0 : 1;
		if (sprite != null) sprite.alpha(close ? 1f : 0.15f);
		return super.act();
	}

	@Override
	public void die(Object cause) {
		flying = false;
		invisible = 0;
		super.die(cause);
	}

	public static class ShadeSprite extends WraithSprite {
		@Override
		public void resetColor() { super.resetColor(); tint(0f, 0f, 0.1f, 0.6f); }
		@Override
		public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
