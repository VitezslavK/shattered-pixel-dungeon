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

package com.shatteredpixel.shatteredpixeldungeon.levels.puzzles;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ScaleArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Frostward;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfDragonsBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BattleAxe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Every path at a fork has its own signature treasure, so choosing a path is a real decision.
// The treasure waits in a puzzle-sealed room on the third floor of the path.
public class RegionTreasure {

	public static final int CAVES_DEPTH = 13;
	public static final int CITY_DEPTH  = 18;

	public static boolean onThisFloor(){
		if (Dungeon.branch != 0) return false;
		return (Dungeon.depth == CAVES_DEPTH && Dungeon.cavesPath >= 0)
				|| (Dungeon.depth == CITY_DEPTH && Dungeon.cityPath >= 0);
	}

	public static ArrayList<Item> create(){
		ArrayList<Item> items = new ArrayList<>();
		if (Dungeon.depth == CAVES_DEPTH){
			Armor armor = new ScaleArmor();
			if (Dungeon.cavesPath == 1){
				//ice caves: frost-warded armor and frost potions
				armor.upgrade(1);
				armor.inscribe(new Frostward());
				items.add(armor);
				items.add(new PotionOfFrost().quantity(2));
			} else {
				//familiar caves: masterwork dwarven armor
				armor.upgrade(2);
				items.add(armor);
			}
		} else {
			Weapon weapon = Random.oneOf(new Longsword(), new BattleAxe(), new Flail());
			if (Dungeon.cityPath == 1){
				//burned city: a weapon that never stopped burning
				weapon.upgrade(1);
				weapon.enchant(new Blazing());
				items.add(weapon);
				items.add(new ElixirOfDragonsBlood());
			} else {
				//dwarven city: masterwork dwarven weapon
				weapon.upgrade(2);
				items.add(weapon);
			}
		}
		for (Item i : items){
			i.cursed = false;
			i.identify(false);
		}
		return items;
	}
}
