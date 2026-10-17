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
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SnakeSprite;
import com.watabou.utils.Random;

//sewer monster that heals itself by draining blood from its victims
public class SewerLeech extends Mob {

	{
		spriteClass = LeechSprite.class;
		HP = HT = 10;
		defenseSkill = 3;
		EXP = 2;
		maxLvl = 7;
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(1, 4); }

	@Override
	public int attackSkill( Char target ) { return 9; }

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc(enemy, damage);
		int heal = Math.min(damage/2, HT - HP);
		if (heal > 0){
			HP += heal;
			if (sprite != null && sprite.visible) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		return damage;
	}

	public static class LeechSprite extends SnakeSprite {
		@Override
		public void resetColor() { super.resetColor(); tint(0.6f, 0.05f, 0.1f, 0.5f); }
		@Override
		public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
