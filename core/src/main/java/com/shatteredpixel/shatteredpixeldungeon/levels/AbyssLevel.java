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

//The rift abyss, the new region below the demon halls (floors 26-29). Dark, cramped by the void,
// and home to the strongest monsters in the dungeon.
public class AbyssLevel extends HallsLevel {

	{
		viewDistance = 5;
		color1 = 0x2a0a3a;
		color2 = 0x6a2a8a;
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_ABYSS;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_ABYSS;
	}
}
