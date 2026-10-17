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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.arena;

import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.ArenaLevels;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

//the arena room used by alternative boss levels, the boss spawns in its center
public class ArenaBossRoom extends StandardRoom {

	@Override
	public float[] sizeCatProbs() {
		return new float[]{0, 1, 0};
	}

	@Override
	public int minWidth() {
		return Math.max(super.minWidth(), 11);
	}

	@Override
	public int minHeight() {
		return Math.max(super.minHeight(), 11);
	}

	@Override
	public boolean canMerge(Level l, Room other, Point p, int mergeTerrain) {
		return false;
	}

	@Override
	public void paint(Level level) {
		Painter.fill( level, this, Terrain.WALL );
		Painter.fill( level, this, 1, Terrain.EMPTY );
		Painter.fill( level, this, 3, Terrain.EMPTY_SP );

		for (Door door : connected.values()) {
			door.set( Door.Type.REGULAR );
		}

		//four pillars to fight around, sometimes absent
		if (Random.Int(3) != 0) {
			int qx = width() / 4, qy = height() / 4;
			Painter.set(level, left + qx, top + qy, Terrain.STATUE);
			Painter.set(level, right - qx, top + qy, Terrain.STATUE);
			Painter.set(level, left + qx, bottom - qy, Terrain.STATUE);
			Painter.set(level, right - qx, bottom - qy, Terrain.STATUE);
		}

		Mob boss = ((ArenaLevels.ArenaLevel) level).createArenaBoss();
		boss.pos = level.pointToCell(center());
		level.mobs.add( boss );
	}

	@Override
	public boolean canPlaceWater(Point p) {
		return false;
	}

	@Override
	public boolean canPlaceTrap(Point p) {
		return false;
	}
}
