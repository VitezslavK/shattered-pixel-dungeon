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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SuccubusSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//abyss monster that steps through the void to appear right next to its prey
public class VoidWalker extends Mob {

	{
		spriteClass = VoidSprite.class;
		HP = HT = 120;
		defenseSkill = 30;
		EXP = 18;
		maxLvl = Hero.MAX_LEVEL - 1;
		properties.add(Property.DEMONIC);
	}

	private int blinkCooldown = 3;

	@Override
	public int damageRoll() { return Random.NormalIntRange(25, 38); }

	@Override
	public int attackSkill( Char target ) { return 42; }

	@Override
	public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 14); }

	@Override
	protected boolean act() {
		if (blinkCooldown > 0) blinkCooldown--;
		if (state == HUNTING && enemy != null && enemy.isAlive() && blinkCooldown <= 0
				&& fieldOfView != null && fieldOfView[enemy.pos]
				&& !Dungeon.level.adjacent(pos, enemy.pos)){
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8){
				int c = enemy.pos + i;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) cells.add(c);
			}
			if (!cells.isEmpty()){
				ScrollOfTeleportation.appear(this, Random.element(cells));
				blinkCooldown = 6;
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	private static final String BLINK = "blink";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(BLINK, blinkCooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		blinkCooldown = bundle.getInt(BLINK);
	}

	public static class VoidSprite extends SuccubusSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(0.2f, 0f, 0.3f, 0.55f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}
}
