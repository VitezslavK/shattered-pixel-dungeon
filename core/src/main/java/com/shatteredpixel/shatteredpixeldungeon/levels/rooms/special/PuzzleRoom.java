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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special;

import com.shatteredpixel.shatteredpixeldungeon.levels.puzzles.RegionTreasure;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.PuzzleNote;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.puzzles.PressurePlate;
import com.shatteredpixel.shatteredpixeldungeon.levels.puzzles.Puzzle;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

//A room holding one of the floor's puzzles. Some puzzles have parts that are placed in other rooms
// (the plate that opens a sealed door, the note with a sequence), so solving them means exploring
// the floor and coming back.
public class PuzzleRoom extends SpecialRoom {

	public Puzzle.Type type;
	private Puzzle puzzle;
	//the path's signature treasure, used instead of a random reward (see RegionTreasure)
	private ArrayList<Item> signature = null;

	public static PuzzleRoom signatureRoom(){
		PuzzleRoom room = new PuzzleRoom(Random.Int(2) == 0 ? Puzzle.Type.GATE : Puzzle.Type.TIMED);
		room.signature = RegionTreasure.create();
		return room;
	}

	//colours of the sequence plates, in the order they are laid out
	public static final int[] SEQUENCE_COLORS = {Trap.RED, Trap.GREEN, Trap.TEAL, Trap.YELLOW};

	public PuzzleRoom(){
		this(Random.element(Puzzle.Type.values()));
	}

	public PuzzleRoom( Puzzle.Type type ){
		this.type = type;
	}

	//roughly every other regular floor gets a puzzle
	public static boolean shouldSpawn(){
		return Dungeon.branch == 0 && Dungeon.depth >= 2 && !Dungeon.bossLevel() && Random.Int(100) < 55;
	}

	public static PuzzleRoom create(){
		//sealed rooms are the simplest, the harder puzzles get more common deeper in
		int roll = Random.Int(100) + Math.min(Dungeon.depth, 20);
		Puzzle.Type type;
		if (roll < 35)       type = Puzzle.Type.GATE;
		else if (roll < 65)  type = Puzzle.Type.TIMED;
		else if (roll < 95)  type = Puzzle.Type.SEQUENCE;
		else                 type = Puzzle.Type.OFFERING;
		if (type == Puzzle.Type.SEQUENCE && Random.Int(2) == 0) type = Puzzle.Type.OFFERING;
		return new PuzzleRoom(type);
	}

	@Override
	public int minWidth() { return type == Puzzle.Type.SEQUENCE || type == Puzzle.Type.OFFERING ? 7 : 5; }
	@Override
	public int minHeight() { return type == Puzzle.Type.SEQUENCE || type == Puzzle.Type.OFFERING ? 7 : 5; }
	@Override
	public int maxWidth() { return 9; }
	@Override
	public int maxHeight() { return 9; }

	@Override
	public void paint( Level level ) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY_SP);

		puzzle = new Puzzle();
		puzzle.type = type;
		puzzle.left = left; puzzle.top = top; puzzle.right = right; puzzle.bottom = bottom;

		Door door = entrance();
		Point c = center();
		int center = level.pointToCell(c);
		puzzle.reward = center;

		switch (type){
			case GATE: case TIMED:
				door.set(Door.Type.LOCKED);
				puzzle.gate = door.x + door.y * level.width();
				for (Item i : signature != null ? signature : Puzzle.rewards(type == Puzzle.Type.TIMED)){
					level.drop(i, center).type = Heap.Type.CHEST;
				}
				Painter.set(level, center, Terrain.PEDESTAL);
				break;

			case SEQUENCE:
				door.set(Door.Type.UNLOCKED); //never a hidden door
				//four plates in a row across the middle, the reward appears next to them
				boolean horizontal = width() >= height();
				int[] plates = new int[SEQUENCE_COLORS.length];
				int start = horizontal
						? Math.max(left+1, Math.min(right-1-(plates.length-1), c.x - plates.length/2))
						: Math.max(top+1, Math.min(bottom-1-(plates.length-1), c.y - plates.length/2));
				for (int i = 0; i < plates.length; i++){
					Point p = horizontal ? new Point(start + i, c.y) : new Point(c.x, start + i);
					plates[i] = level.pointToCell(p);
				}
				puzzle.plates = plates;
				for (int i = 0; i < plates.length; i++){
					Painter.set(level, plates[i], Terrain.TRAP);
					level.setTrap(new PressurePlate().setColor(SEQUENCE_COLORS[i]), plates[i]);
				}
				ArrayList<Integer> order = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
				Collections.shuffle(order, new java.util.Random(Random.Long()));
				puzzle.order = new int[]{order.get(0), order.get(1), order.get(2), order.get(3)};
				Point r = horizontal ? new Point(c.x, Math.max(top+1, c.y-1)) : new Point(Math.max(left+1, c.x-1), c.y);
				puzzle.reward = level.pointToCell(r);
				Painter.set(level, puzzle.reward, Terrain.PEDESTAL);
				break;

			case OFFERING:
				door.set(Door.Type.UNLOCKED); //never a hidden door
				int[] pedestals = new int[3];
				boolean wide = width() >= height();
				for (int i = 0; i < 3; i++){
					Point p = wide ? new Point(c.x - 2 + i*2, c.y) : new Point(c.x, c.y - 2 + i*2);
					p.x = Math.max(left+1, Math.min(right-1, p.x));
					p.y = Math.max(top+1, Math.min(bottom-1, p.y));
					pedestals[i] = level.pointToCell(p);
					Painter.set(level, pedestals[i], Terrain.PEDESTAL);
				}
				puzzle.pedestals = pedestals;
				ArrayList<String> needs = new ArrayList<>(Arrays.asList(Puzzle.OFFERINGS));
				Collections.shuffle(needs, new java.util.Random(Random.Long()));
				puzzle.needs = new String[]{needs.get(0), needs.get(1), needs.get(2)};
				Point rp = wide ? new Point(c.x, Math.max(top+1, c.y-2)) : new Point(Math.max(left+1, c.x-2), c.y);
				puzzle.reward = level.pointToCell(rp);
				break;
		}

		level.puzzles.add(puzzle);
	}

	//places the parts of the puzzle that live outside of its room, called once the whole level is painted
	public static void placeRemoteParts( RegularLevel level, ArrayList<Room> rooms ){
		for (Room r : rooms){
			if (!(r instanceof PuzzleRoom)) continue;
			PuzzleRoom room = (PuzzleRoom) r;
			if (room.puzzle == null) continue;
			if (room.type == Puzzle.Type.GATE || room.type == Puzzle.Type.TIMED){
				int cell = remoteCell(level, rooms, room);
				if (cell == -1){
					//no good spot elsewhere, put the plate right next to the door so the puzzle is still solvable
					cell = cellOutsideDoor(level, room);
				}
				if (cell == -1) continue;
				Painter.set(level, cell, Terrain.TRAP);
				level.setTrap(new PressurePlate().setColor(room.type == Puzzle.Type.TIMED ? Trap.ORANGE : Trap.WHITE), cell);
				room.puzzle.plates = new int[]{cell};
			} else if (room.type == Puzzle.Type.SEQUENCE){
				int cell = remoteCell(level, rooms, room);
				if (cell == -1) cell = room.puzzle.reward;
				PuzzleNote note = new PuzzleNote();
				note.order = room.puzzle.order.clone();
				level.drop(note, cell);
			}
		}
	}

	//a free floor cell in a standard room, preferably far from the puzzle room
	private static int remoteCell( Level level, ArrayList<Room> rooms, PuzzleRoom from ){
		ArrayList<Room> candidates = new ArrayList<>();
		for (Room r : rooms){
			if (r instanceof StandardRoom && r != from && r.width() > 2 && r.height() > 2){
				candidates.add(r);
			}
		}
		if (candidates.isEmpty()) return -1;
		Point origin = from.center();
		Collections.sort(candidates, (a, b) -> {
			Point pa = a.center(), pb = b.center();
			int da = Math.abs(pa.x-origin.x) + Math.abs(pa.y-origin.y);
			int db = Math.abs(pb.x-origin.x) + Math.abs(pb.y-origin.y);
			return db - da;
		});
		//one of the farther rooms, but not always the very farthest
		int pickFrom = Math.max(1, candidates.size()/2);
		for (int attempt = 0; attempt < 30; attempt++){
			Room r = candidates.get(Random.Int(pickFrom));
			Point p = r.random(1);
			int cell = level.pointToCell(p);
			if (freeFloor(level, cell)) return cell;
		}
		for (Room r : candidates){
			for (Point p : r.getPoints()){
				int cell = level.pointToCell(p);
				if (r.inside(p) && freeFloor(level, cell)) return cell;
			}
		}
		return -1;
	}

	private static boolean freeFloor( Level level, int cell ){
		int t = level.map[cell];
		return (t == Terrain.EMPTY || t == Terrain.EMPTY_SP || t == Terrain.EMPTY_DECO || t == Terrain.GRASS)
				&& level.traps.get(cell) == null && level.heaps.get(cell) == null
				&& level.plants.get(cell) == null && !nearTransition(level, cell)
				&& level.findMob(cell) == null;
	}

	//level.getTransition relies on Dungeon.level, which isn't set yet while a level is being built
	private static boolean nearTransition( Level level, int cell ){
		int x = cell % level.width(), y = cell / level.width();
		for (LevelTransition t : level.transitions){
			if (x >= t.left-1 && x <= t.right+1 && y >= t.top-1 && y <= t.bottom+1) return true;
		}
		return false;
	}

	private static int cellOutsideDoor( Level level, PuzzleRoom room ){
		Door door = room.entrance();
		int doorCell = door.x + door.y * level.width();
		for (int n : new int[]{-1, 1, -level.width(), level.width()}){
			int cell = doorCell + n;
			if (cell >= 0 && cell < level.length() && !room.inside(level.cellToPoint(cell)) && freeFloor(level, cell)){
				return cell;
			}
		}
		return -1;
	}
}
