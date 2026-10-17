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

import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.Builder;
import com.shatteredpixel.shatteredpixeldungeon.levels.builders.FigureEightBuilder;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.arena.ArenaBossRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.arena.ArenaExitRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.entrance.EntranceRoom;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.Callback;
import com.watabou.utils.DeviceCompat;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Shared logic for alternative boss levels. These are regular levels from their region with a
// boss arena room, and an exit that is locked until the boss drops its key.
public class ArenaLevels {

	public interface ArenaLevel {
		Mob createArenaBoss();
	}

	//0 is the original boss level, 1 is the alternative arena level
	public static int bossVariant( int depth ){
		//debug builds always use the newest bosses, to make testing easier
		if (DeviceCompat.isDebug()) return 1;
		return (int)Math.floorMod(Dungeon.seedForDepth(depth, 0) / 7919, 2L);
	}

	static ArrayList<Room> initRooms( RegularLevel level ){
		ArrayList<Room> initRooms = new ArrayList<>();

		initRooms.add( level.roomEntrance = EntranceRoom.createEntrance() );
		initRooms.add( level.roomExit = new ArenaExitRoom() );

		for (int i = 0; i < 3; i++) {
			StandardRoom s = StandardRoom.createRoom();
			s.setSizeCat(0, 0);
			initRooms.add(s);
		}

		ArenaBossRoom arena = new ArenaBossRoom();
		initRooms.add(arena);
		((FigureEightBuilder)level.builder).setLandmarkRoom(arena);
		return initRooms;
	}

	static Builder builder(){
		return new FigureEightBuilder()
				.setLoopShape( 2 , Random.Float(0.3f, 0.8f), 0f)
				.setPathLength(1f, new float[]{1})
				.setTunnelLength(new float[]{1, 2}, new float[]{1});
	}

	static void createItems( RegularLevel level ){
		Random.pushGenerator(Random.Long());
			ArrayList<Item> bonesItems = Bones.get();
			if (bonesItems != null) {
				int pos;
				do {
					pos = level.pointToCell(level.roomEntrance.random());
				} while (pos == level.entrance() || level.solid[pos]);
				for (Item i : bonesItems) {
					level.drop(i, pos).setHauntedIfCursed().type = Heap.Type.REMAINS;
				}
			}
		Random.popGenerator();
	}

	static int randomRespawnCell( RegularLevel level, Char ch ){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (Point p : level.roomEntrance.getPoints()){
			int cell = level.pointToCell(p);
			if (level.passable[cell]
					&& level.roomEntrance.inside(p)
					&& Actor.findChar(cell) == null
					&& (!Char.hasProp(ch, Char.Property.LARGE) || level.openSpace[cell])){
				candidates.add(cell);
			}
		}
		return candidates.isEmpty() ? -1 : Random.element(candidates);
	}

	static boolean bossAlive( Level level ){
		for (Mob m : level.mobs){
			if (Char.hasProp(m, Char.Property.BOSS)) return true;
		}
		return false;
	}

	static void onSeal( String bossMusic ){
		Statistics.qualifiedForBossChallengeBadge = true;
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.play(bossMusic, true);
			}
		});
	}

	static void onUnseal(){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				Music.INSTANCE.fadeOut(5f, new Callback() {
					@Override
					public void call() {
						Music.INSTANCE.end();
					}
				});
			}
		});
	}
}
