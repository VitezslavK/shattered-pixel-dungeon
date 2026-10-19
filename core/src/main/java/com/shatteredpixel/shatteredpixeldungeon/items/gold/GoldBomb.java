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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//A bomb packed with gold leaf: the blast blinds everything nearby and scatters some coins.
public class GoldBomb extends Bomb {

	{
		image = ItemSpriteSheet.GOLD_BOMB;
	}

	@Override
	public void explode(int cell) {
		super.explode(cell);

		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				Char ch = Actor.findChar(i);
				if (ch != null && ch.isAlive() && ch != Dungeon.hero){
					Buff.prolong(ch, Blindness.class, 5f);
				}
				if (Dungeon.level.heroFOV[i] && Random.Int(3) == 0){
					CellEmitter.get(i).burst(Speck.factory(Speck.COIN), 2);
				}
			}
		}

		//some of the gold survives the blast
		ArrayList<Integer> spots = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9){
			int c = cell + n;
			if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c]) spots.add(c);
		}
		int piles = Math.min(spots.size(), 2 + Random.Int(2));
		for (int i = 0; i < piles; i++){
			int c = spots.remove(Random.Int(spots.size()));
			Heap h = Dungeon.level.drop(new Gold(Random.IntRange(3, 6) + Dungeon.depth), c);
			if (h.sprite != null) h.sprite.drop(c);
		}
	}

	@Override
	public int value() {
		return quantity * (20 + 40);
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs =  new Class[]{GoldIngot.class, Bomb.class};
			inQuantity = new int[]{1, 1};
			cost = 2;
			output = GoldBomb.class;
			outQuantity = 1;
		}
	}
}
