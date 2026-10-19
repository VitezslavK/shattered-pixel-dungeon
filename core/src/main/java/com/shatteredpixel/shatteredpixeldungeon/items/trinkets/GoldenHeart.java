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
package com.shatteredpixel.shatteredpixeldungeon.items.trinkets;

import com.shatteredpixel.shatteredpixeldungeon.items.gold.GoldIngot;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;

//A heart of beaten gold that beats faster around coins: picking up gold heals its bearer.
public class GoldenHeart extends Trinket {

	{
		image = ItemSpriteSheet.GOLDEN_HEART;
	}

	@Override
	protected int upgradeEnergyCost() {
		return 6+2*level();
	}

	@Override
	public String statsDesc() {
		if (isIdentified()){
			return Messages.get(this, "stats_desc", percent(buffedLvl()));
		} else {
			return Messages.get(this, "typical_stats_desc", percent(0));
		}
	}

	//% of the gold picked up that is turned into health
	public static int percent( int level ){
		return level < 0 ? 0 : 10 + 5*level;
	}

	public static int healFor( int gold ){
		int level = trinketLevel(GoldenHeart.class);
		if (level < 0) return 0;
		return Math.max(1, Math.round(gold * percent(level) / 100f));
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs =  new Class[]{GoldIngot.class, PotionOfHealing.class};
			inQuantity = new int[]{4, 1};
			cost = 6;
			output = GoldenHeart.class;
			outQuantity = 1;
		}
	}
}
