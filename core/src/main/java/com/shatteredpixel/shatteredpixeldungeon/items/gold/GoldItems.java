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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.GoldenHeart;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.GoldenIdol;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.MerchantsSeal;
import com.watabou.utils.Random;

//The family of golden items: made from gold ingots in the alchemy pot, bought at golden altars,
// and sometimes stocked by the shopkeeper. Gold is otherwise useless before the first shop.
public class GoldItems {

	public static final int INGOT_PRICE = 40;

	public enum Kind { EQUIPMENT, CONSUMABLE, TALISMAN }

	//golden weapons follow the depth: a dagger early, a sabre in the middle, the hammer deep down
	public static Item equipment( int depth ){
		if (depth <= 9)  return new GildedDagger();
		if (depth <= 19) return new GoldenSabre();
		return new MidasHammer();
	}

	public static Item consumable(){
		switch (Random.Int(3)){
			case 0: default: return new GoldBomb();
			case 1: return new LuckyCoin().quantity(2);
			case 2: return new ElixirOfGoldenSkin();
		}
	}

	public static Item talisman(){
		switch (Random.Int(3)){
			case 0: default: return new GoldenIdol();
			case 1: return new MerchantsSeal();
			case 2: return new GoldenHeart();
		}
	}

	public static Item random( Kind kind, int depth ){
		Item i;
		switch (kind){
			case EQUIPMENT: default: i = equipment(depth); break;
			case CONSUMABLE: i = consumable(); break;
			case TALISMAN: i = talisman(); break;
		}
		return i.identify(false);
	}

	//gold dropped by an enemy slain with a golden weapon
	public static void dropKillGold( Char defender, int damage, float scale ){
		if (!(defender instanceof Mob) || defender.alignment != Char.Alignment.ENEMY) return;
		if (damage < defender.HP) return;
		int amount = Math.round(Random.IntRange(2 + Dungeon.depth, 6 + 3 * Dungeon.depth) * scale);
		if (amount <= 0) return;
		com.shatteredpixel.shatteredpixeldungeon.items.Heap heap = Dungeon.level.drop(new Gold(amount), defender.pos);
		if (heap.sprite != null) heap.sprite.drop();
	}
}
