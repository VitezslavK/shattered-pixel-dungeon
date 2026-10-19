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

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;

import java.util.ArrayList;

//Gilding: gold ingots + any melee weapon = the golden weapon of its tier, keeping its upgrades,
// enchantment and augment. Tiers 1-2 need 2 ingots, 3-4 need 4, tier 5 needs 6.
public class GildRecipe extends Recipe {

	public static boolean isGolden( Item item ){
		return item instanceof GildedDagger || item instanceof GoldenSabre || item instanceof MidasHammer;
	}

	public static boolean gildable( Item item ){
		return item instanceof MeleeWeapon && !(item instanceof MagesStaff) && !isGolden(item);
	}

	private static int ingotsFor( MeleeWeapon w ){
		if (w.tier <= 2) return 2;
		if (w.tier <= 4) return 4;
		return 6;
	}

	private static MeleeWeapon weapon( ArrayList<Item> ingredients ){
		for (Item i : ingredients) if (gildable(i)) return (MeleeWeapon) i;
		return null;
	}

	private static GoldIngot ingots( ArrayList<Item> ingredients ){
		for (Item i : ingredients) if (i instanceof GoldIngot) return (GoldIngot) i;
		return null;
	}

	@Override
	public boolean testIngredients(ArrayList<Item> ingredients) {
		if (ingredients.size() != 2) return false;
		MeleeWeapon w = weapon(ingredients);
		GoldIngot g = ingots(ingredients);
		return w != null && g != null && g.quantity() >= ingotsFor(w);
	}

	@Override
	public int cost(ArrayList<Item> ingredients) {
		return 3;
	}

	@Override
	public Item brew(ArrayList<Item> ingredients) {
		if (!testIngredients(ingredients)) return null;
		MeleeWeapon w = weapon(ingredients);
		GoldIngot g = ingots(ingredients);
		Item result = sampleOutput(ingredients);
		g.quantity(g.quantity() - ingotsFor(w));
		w.quantity(0);
		return result;
	}

	@Override
	public Item sampleOutput(ArrayList<Item> ingredients) {
		MeleeWeapon w = weapon(ingredients);
		MeleeWeapon out;
		if (w == null || w.tier <= 2)  out = new GildedDagger();
		else if (w.tier <= 4)          out = new GoldenSabre();
		else                           out = new MidasHammer();
		if (w != null){
			out.level(w.level());
			out.enchantment = w.enchantment;
			out.augment = w.augment;
			out.levelKnown = w.levelKnown;
			out.cursedKnown = w.cursedKnown;
		}
		return out;
	}
}
