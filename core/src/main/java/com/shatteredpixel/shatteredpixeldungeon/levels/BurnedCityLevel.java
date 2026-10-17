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

package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Emberbound;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

//An optional branch of the dwarven city (floors 16-19), chosen when leaving floor 15. The city
// still smoulders: its monsters are emberbound and set their targets on fire, but loot is richer.
public class BurnedCityLevel extends CityLevel {

	{
		color1 = 0x6a2010;
		color2 = 0xd06020;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_BURNED;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_BURNED;
	}

	@Override
	public Mob createMob() {
		Mob m = super.createMob();
		Buff.affect(m, Emberbound.class);
		return m;
	}

	@Override
	protected void createItems() {
		super.createItems();

		Random.pushGenerator(Random.Long());
			//scorch some of the floor
			for (int i = 0; i < length(); i++){
				if (map[i] == Terrain.EMPTY && Random.Int(6) == 0){
					map[i] = Terrain.EMBERS;
				}
			}
			//the ruins hold more forgotten treasure
			Item extra = Generator.random();
			if (extra != null){
				drop(extra, randomDropCell());
			}
		Random.popGenerator();
	}
}
