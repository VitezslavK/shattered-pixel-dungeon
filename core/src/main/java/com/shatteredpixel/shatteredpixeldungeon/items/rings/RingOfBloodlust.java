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

package com.shatteredpixel.shatteredpixeldungeon.items.rings;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//heals the wearer for a portion of the damage dealt by their melee attacks
public class RingOfBloodlust extends Ring {

	{
		icon = ItemSpriteSheet.Icons.RING_FORCE;
		buffClass = Bloodlust.class;
	}

	public String statsInfo() {
		if (isIdentified()){
			String info = Messages.get(this, "stats", Messages.decimalFormat("#.##", 100f * lifesteal(soloBuffedBonus())));
			if (isEquipped(Dungeon.hero) && soloBuffedBonus() != combinedBuffedBonus(Dungeon.hero)){
				info += "\n\n" + Messages.get(this, "combined_stats",
						Messages.decimalFormat("#.##", 100f * lifesteal(combinedBuffedBonus(Dungeon.hero))));
			}
			return info;
		} else {
			return Messages.get(this, "typical_stats", Messages.decimalFormat("#.##", 100f * lifesteal(1)));
		}
	}

	public String upgradeStat1(int level){
		if (cursed && cursedKnown) level = Math.min(-1, level-3);
		return Messages.decimalFormat("#.##", 100f * lifesteal(level+1)) + "%";
	}

	private static float lifesteal( int bonus ){
		//4% at +1, approaching 30%. Cursed rings drain the wearer instead.
		if (bonus >= 0) return 0.30f * (1f - (float)Math.pow(0.87f, bonus));
		return -0.04f * -bonus;
	}

	@Override
	protected RingBuff buff( ) {
		return new Bloodlust();
	}

	//called from Talent.onAttackProc via the hero's melee attacks
	public static int lifestealFor( Char attacker, int damage ){
		int bonus = getBuffedBonus(attacker, Bloodlust.class);
		if (bonus == 0) return 0;
		return Math.round(damage * lifesteal(bonus));
	}

	public class Bloodlust extends RingBuff {
	}
}
