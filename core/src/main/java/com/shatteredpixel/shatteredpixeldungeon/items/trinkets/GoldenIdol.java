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
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;

//A small golden idol of a forgotten god of wealth: every pile of gold you pick up grows.
public class GoldenIdol extends Trinket {

	{
		image = ItemSpriteSheet.GOLDEN_IDOL;
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

	//% more gold picked up
	public static int percent( int level ){
		return level < 0 ? 0 : 20 + 15*level;
	}

	public static float goldMultiplier(){
		return 1f + percent(trinketLevel(GoldenIdol.class))/100f;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs =  new Class[]{GoldIngot.class, ScrollOfIdentify.class};
			inQuantity = new int[]{4, 1};
			cost = 6;
			output = GoldenIdol.class;
			outQuantity = 1;
		}
	}
}
