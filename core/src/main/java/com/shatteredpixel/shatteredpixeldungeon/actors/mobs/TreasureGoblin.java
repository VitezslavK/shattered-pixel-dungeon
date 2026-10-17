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
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GnollTricksterSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

//floor event monster: a goblin hauling treasure that runs away as soon as it sees the hero
public class TreasureGoblin extends Mob {

	{
		spriteClass = GoblinSprite.class;
		HP = HT = 15 + 3*Dungeon.depth;
		defenseSkill = 6 + Dungeon.depth;
		EXP = Math.max(1, Dungeon.depth/2);
		baseSpeed = 1.2f;
		lootChance = 0;
	}

	private int fleeTime = 0;

	@Override
	public int damageRoll() { return 1; }

	@Override
	public int attackSkill(Char target) { return 1; }

	@Override
	protected boolean act() {
		if (state == HUNTING){
			state = FLEEING;
			GLog.i(Messages.get(this, "flees"));
		}
		if (state == FLEEING){
			fleeTime++;
			//escapes once it has run for a while out of sight
			if (fleeTime > 30 && !Dungeon.level.heroFOV[pos]){
				GLog.w(Messages.get(this, "escaped"));
				destroy();
				sprite.killAndErase();
				return true;
			}
		}
		return super.act();
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		for (int i = 0; i < 2; i++){
			Item loot = Generator.random();
			if (loot != null) Dungeon.level.drop(loot, pos).sprite.drop();
		}
		Dungeon.level.drop(new Gold().random(), pos).sprite.drop();
	}

	private static final String FLEE = "flee";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(FLEE, fleeTime);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		fleeTime = bundle.getInt(FLEE);
	}

	public static class GoblinSprite extends GnollTricksterSprite {
		@Override
		public void resetColor() { super.resetColor(); tint(1f, 0.8f, 0.1f, 0.4f); }
		@Override
		public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
