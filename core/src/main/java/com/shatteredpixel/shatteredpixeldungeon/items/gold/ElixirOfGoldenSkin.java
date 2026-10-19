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
package com.shatteredpixel.shatteredpixeldungeon.items.gold;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Liquid gold and a healing potion: heals fully and gilds the skin into armor for a while.
public class ElixirOfGoldenSkin extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_GOLDSKIN;
	}

	@Override
	public void apply(Hero hero) {
		PotionOfHealing.cure(hero);
		PotionOfHealing.heal(hero);
		Buff.affect(hero, Barkskin.class).set(2 + hero.lvl / 2, 15);
		GLog.p(Messages.get(this, "msg"));
	}

	@Override
	public int value() {
		return quantity * (30 + 80);
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs =  new Class[]{GoldIngot.class, PotionOfHealing.class};
			inQuantity = new int[]{2, 1};
			cost = 4;
			output = ElixirOfGoldenSkin.class;
			outQuantity = 1;
		}
	}
}
