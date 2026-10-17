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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//State and rules of one puzzle on a level. Puzzles are stored on the level they belong to and are
// driven by pressure plates (see PressurePlate) and by the hero's turns (see Hero.act).
public class Puzzle implements Bundlable {

	public enum Type {
		GATE,       //a plate somewhere on the floor opens a sealed treasure room
		TIMED,      //like GATE, but the door only stays open for a short while
		SEQUENCE,   //coloured plates must be pressed in the order written on a note found elsewhere
		OFFERING    //pedestals that each ask for a kind of item, gathered from around the floor
	}

	//item categories the offering altar can ask for
	public static final String[] OFFERINGS = {"potion", "scroll", "seed", "stone", "food", "thrown"};

	public Type type = Type.GATE;
	public int gate = -1;
	public int[] plates = new int[0];
	public int[] order = new int[0];
	public int progress = 0;
	public int[] pedestals = new int[0];
	public String[] needs = new String[0];
	public int reward = -1;
	public int left, top, right, bottom;
	public int timer = 0;
	public boolean open = false;
	public boolean solved = false;
	public boolean heroInside = false;

	public boolean inside( int cell, Level level ){
		int x = cell % level.width(), y = cell / level.width();
		return x > left && x < right && y > top && y < bottom;
	}

	public static Puzzle forPlate( Level level, int cell ){
		for (Puzzle p : level.puzzles){
			for (int plate : p.plates) if (plate == cell) return p;
		}
		return null;
	}

	//puzzle gates can't be opened by keys, only by solving their puzzle
	public static boolean isGate( Level level, int cell ){
		if (level == null) return false;
		for (Puzzle p : level.puzzles){
			if (p.gate == cell && !p.open) return true;
		}
		return false;
	}

	//called when the hero (or anything else) presses a plate
	public void onPlate( int cell ){
		if (solved && type != Type.TIMED) return;
		switch (type){
			case GATE:
				openGate();
				solved = true;
				GLog.p(Messages.get(Puzzle.class, "gate_opened"));
				break;
			case TIMED:
				if (solved) return;
				int turns = timedTurns(cell);
				openGate();
				timer = turns;
				GLog.w(Messages.get(Puzzle.class, "timed_opened", turns));
				break;
			case SEQUENCE:
				int index = -1;
				for (int i = 0; i < plates.length; i++) if (plates[i] == cell) index = i;
				if (index < 0) return;
				if (order[progress] == index){
					progress++;
					Sample.INSTANCE.play(Assets.Sounds.EVOKE);
					fx(cell).burst(Speck.factory(Speck.LIGHT), 6);
					if (progress >= order.length){
						solved = true;
						for (int p : plates){
							Trap t = Dungeon.level.traps.get(p);
							if (t != null) t.disarm();
						}
						GLog.p(Messages.get(Puzzle.class, "sequence_solved"));
						spawnReward(true);
					} else {
						GLog.i(Messages.get(Puzzle.class, "sequence_progress", progress, order.length));
					}
				} else {
					progress = 0;
					GLog.n(Messages.get(Puzzle.class, "sequence_wrong"));
					Sample.INSTANCE.play(Assets.Sounds.ALERT);
					//the mechanism's grinding alerts the floor
					for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
						if (mob.alignment != com.shatteredpixel.shatteredpixeldungeon.actors.Char.Alignment.ALLY
								&& mob.sprite != null && Dungeon.level.distance(mob.pos, cell) <= 12){
							mob.beckon(cell);
						}
					}
				}
				break;
			case OFFERING:
				break;
		}
	}

	//how long the timed door stays open: enough to walk back to it at a steady pace
	private int timedTurns( int plate ){
		Level level = Dungeon.level;
		boolean[] passable = BArray.or(level.passable, level.avoid, null);
		passable[gate] = true;
		PathFinder.buildDistanceMap(gate, passable);
		int dist = PathFinder.distance[plate];
		if (dist == Integer.MAX_VALUE) dist = level.distance(plate, gate) * 2;
		return Math.round(dist * 1.4f) + 6;
	}

	private void openGate(){
		if (open || gate < 0) return;
		open = true;
		Level.set(gate, Terrain.DOOR);
		GameScene.updateMap(gate);
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
		fx(gate).burst(Speck.factory(Speck.STAR), 8);
	}

	private boolean closeGate(){
		if (!open || gate < 0) return true;
		if (Actor.findChar(gate) != null || Dungeon.level.heaps.get(gate) != null) return false;
		open = false;
		Level.set(gate, Terrain.LOCKED_DOOR);
		GameScene.updateMap(gate);
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.OPEN);
		return true;
	}

	private static float lastTick = -1;

	//visual effects only exist while the game scene is showing
	private static com.watabou.noosa.particles.Emitter fx( int cell ){
		if (!GameScene.sceneActive()){
			return new com.watabou.noosa.particles.Emitter();
		}
		return CellEmitter.get(cell);
	}

	//called from Hero.act, runs the puzzles of the current level once per game turn
	public static void tick( Hero hero ){
		if (Dungeon.level == null || Dungeon.level.puzzles.isEmpty()) return;
		float now = Actor.now();
		if (now < lastTick + 1f && now >= lastTick) return;
		lastTick = now;
		for (Puzzle p : Dungeon.level.puzzles){
			p.onHeroTurn(hero);
		}
	}

	//called once per turn while the hero is on the puzzle's level
	public void onHeroTurn( Hero hero ){
		boolean nowInside = inside(hero.pos, Dungeon.level);
		boolean entered = nowInside && !heroInside;
		heroInside = nowInside;

		switch (type){
			case TIMED:
				if (open && !solved){
					if (nowInside){
						solved = true;
						GLog.p(Messages.get(Puzzle.class, "timed_made_it"));
					} else if (--timer <= 0 && closeGate()){
						GLog.n(Messages.get(Puzzle.class, "timed_closed"));
					} else if (timer == 5){
						GLog.w(Messages.get(Puzzle.class, "timed_hurry"));
					}
				}
				break;
			case SEQUENCE:
				if (entered && !solved) GLog.i(Messages.get(Puzzle.class, "sequence_hint"));
				break;
			case OFFERING:
				if (solved) break;
				if (entered) GLog.i(Messages.get(Puzzle.class, "offering_hint", needsText()));
				checkOfferings();
				break;
			default:
				break;
		}
	}

	public String needsText(){
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < needs.length; i++){
			if (i > 0) b.append(", ");
			b.append("_").append(Messages.get(Puzzle.class, "offer_" + needs[i])).append("_");
		}
		return b.toString();
	}

	public static boolean matches( Item item, String need ){
		switch (need){
			case "potion": return item instanceof Potion;
			case "scroll": return item instanceof Scroll;
			case "seed":   return item instanceof Plant.Seed;
			case "stone":  return item instanceof Runestone;
			case "food":   return item instanceof Food;
			case "thrown": return item instanceof MissileWeapon;
		}
		return false;
	}

	private void checkOfferings(){
		boolean[] used = new boolean[pedestals.length];
		//every need has to be met by an item on a different pedestal
		for (String need : needs){
			boolean found = false;
			for (int i = 0; i < pedestals.length; i++){
				if (used[i]) continue;
				Heap heap = Dungeon.level.heaps.get(pedestals[i]);
				if (heap != null && heap.type == Heap.Type.HEAP && matches(heap.peek(), need)){
					used[i] = true;
					found = true;
					break;
				}
			}
			if (!found) return;
		}
		for (int i = 0; i < pedestals.length; i++){
			if (!used[i]) continue;
			Heap heap = Dungeon.level.heaps.get(pedestals[i]);
			if (heap != null){
				heap.pickUp();
				if (heap.isEmpty()) heap.destroy();
				fx(pedestals[i]).burst(Speck.factory(Speck.LIGHT), 8);
			}
		}
		solved = true;
		GLog.p(Messages.get(Puzzle.class, "offering_solved"));
		spawnReward(true);
	}

	private void spawnReward( boolean big ){
		int cell = reward >= 0 ? reward : (plates.length > 0 ? plates[0] : Dungeon.hero.pos);
		for (Item i : rewards(big)){
			Heap heap = Dungeon.level.drop(i, cell);
			if (heap.sprite != null) heap.sprite.drop(cell);
		}
		fx(cell).burst(Speck.factory(Speck.STAR), 12);
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP);
	}

	//the prize for solving a puzzle, the harder ones give better items
	public static ArrayList<Item> rewards( boolean big ){
		ArrayList<Item> items = new ArrayList<>();
		Generator.Category[] gear = {Generator.Category.WAND, Generator.Category.RING, Generator.Category.ARTIFACT};
		Item prize = null;
		for (int tries = 0; prize == null && tries < 5; tries++){
			prize = Generator.random(Random.oneOf(gear));
		}
		if (prize == null) prize = Generator.random(Generator.Category.RING);
		prize.cursed = false;
		items.add(prize);
		items.add(Generator.random(Random.oneOf(Generator.Category.POTION, Generator.Category.SCROLL)));
		if (big){
			items.add(Generator.random(Random.oneOf(Generator.Category.POTION, Generator.Category.SCROLL, Generator.Category.STONE)));
			items.add(new Gold().random());
		}
		return items;
	}

	private static final String TYPE = "type";
	private static final String GATE = "gate";
	private static final String PLATES = "plates";
	private static final String ORDER = "order";
	private static final String PROGRESS = "progress";
	private static final String PEDESTALS = "pedestals";
	private static final String NEEDS = "needs";
	private static final String REWARD = "reward";
	private static final String BOUNDS = "bounds";
	private static final String TIMER = "timer";
	private static final String OPEN = "open";
	private static final String SOLVED = "solved";
	private static final String INSIDE = "inside";

	@Override
	public void storeInBundle( Bundle bundle ) {
		bundle.put(TYPE, type);
		bundle.put(GATE, gate);
		bundle.put(PLATES, plates);
		bundle.put(ORDER, order);
		bundle.put(PROGRESS, progress);
		bundle.put(PEDESTALS, pedestals);
		bundle.put(NEEDS, needs);
		bundle.put(REWARD, reward);
		bundle.put(BOUNDS, new int[]{left, top, right, bottom});
		bundle.put(TIMER, timer);
		bundle.put(OPEN, open);
		bundle.put(SOLVED, solved);
		bundle.put(INSIDE, heroInside);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		type = bundle.getEnum(TYPE, Type.class);
		gate = bundle.getInt(GATE);
		plates = bundle.getIntArray(PLATES);
		order = bundle.getIntArray(ORDER);
		progress = bundle.getInt(PROGRESS);
		pedestals = bundle.getIntArray(PEDESTALS);
		needs = bundle.getStringArray(NEEDS);
		reward = bundle.getInt(REWARD);
		int[] b = bundle.getIntArray(BOUNDS);
		left = b[0]; top = b[1]; right = b[2]; bottom = b[3];
		timer = bundle.getInt(TIMER);
		open = bundle.getBoolean(OPEN);
		solved = bundle.getBoolean(SOLVED);
		heroInside = bundle.getBoolean(INSIDE);
	}
}
