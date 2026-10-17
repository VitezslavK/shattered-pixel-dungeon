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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frostbound;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CavesPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Random;

//An optional branch of the caves (floors 11-14), chosen when leaving floor 10. Its monsters are
// frostbound and chill on hit, there is far more water, but each floor holds extra loot.
public class IceCavesLevel extends CavesLevel {

	{
		color1 = 0x6a8aa8;
		color2 = 0x9cc4e4;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_ICE;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_ICE;
	}

	@Override
	protected Painter painter() {
		return new CavesPainter()
				.setWater(feeling == Feeling.WATER ? 0.9f : 0.55f, 6)
				.setGrass(0.05f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}

	@Override
	public Mob createMob() {
		Mob m = super.createMob();
		Buff.affect(m, Frostbound.class);
		return m;
	}

	@Override
	protected void createItems() {
		super.createItems();
		//the ice preserves more of what fell down here
		Random.pushGenerator(Random.Long());
			Item extra = Generator.random();
			if (extra != null){
				drop(extra, randomDropCell());
			}
		Random.popGenerator();
	}
}
