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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.VoidHerald;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//floor 30, the void herald's arena guarding the way down to the amulet
public class AbyssArenaLevel extends AbyssLevel implements ArenaLevels.ArenaLevel {

	{
		viewDistance = 8;
	}

	@Override
	public Mob createArenaBoss() {
		return new VoidHerald();
	}

	@Override
	public void playLevelMusic() {
		if (locked){
			Music.INSTANCE.play(Assets.Music.HALLS_BOSS, true);
		} else if (ArenaLevels.bossAlive(this)){
			Music.INSTANCE.end();
		} else {
			super.playLevelMusic();
		}
	}

	@Override
	protected ArrayList<Room> initRooms() {
		return ArenaLevels.initRooms(this);
	}

	@Override
	protected Builder builder() {
		return ArenaLevels.builder();
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		return 3;
	}

	@Override
	protected int specialRooms(boolean forceMax) {
		return 0;
	}

	@Override
	protected int nTraps() {
		return 0;
	}

	@Override
	protected void createMobs() {
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
		ArenaLevels.createItems(this);
	}

	@Override
	public int randomRespawnCell( Char ch ) {
		return ArenaLevels.randomRespawnCell(this, ch);
	}

	@Override
	public void seal() {
		if (!locked) {
			super.seal();
			ArenaLevels.onSeal(Assets.Music.HALLS_BOSS);
		}
	}

	@Override
	public void unseal() {
		if (locked) {
			super.unseal();
			ArenaLevels.onUnseal();
		}
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		roomExit = roomEntrance;
	}
}
