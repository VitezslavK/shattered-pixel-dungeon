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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.utils.Random;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

//Automated playthrough used to test the real game (scenes, transitions, rendering, saving).
// Only active when the game is launched with -Dspd.autotour=<HERO CLASS>, never in normal play.
// It starts a run, kills bosses, takes the stairs on every floor and picks random branches,
// printing progress as "AUTOTOUR ..." lines and exiting when the last floor is reached.
public class AutoTour {

	public static boolean enabled(){
		return System.getProperty("spd.autotour") != null;
	}

	private static boolean started = false;
	private static int lastDepth = -1;
	private static float levelTime = 0;
	private static float idle = 0;
	private static float statusTimer = 0;

	private static void log( String msg ){
		System.out.println("AUTOTOUR " + msg);
		System.out.flush();
	}

	//called at the end of TitleScene.create
	public static void onTitle(){
		if (!enabled()) return;
		if (started){
			//the title scene can be created twice while the game starts up
			if (lastDepth == -1) return;
			log("BACK AT TITLE depth=" + Dungeon.depth + " (game over or quit)");
			Game.instance.finish();
			return;
		}
		started = true;
		Thread watchdog = new Thread(() -> {
			while (true){
				try { Thread.sleep(5000); } catch (InterruptedException e){ return; }
				com.watabou.noosa.Scene sc = Game.scene();
				StringBuilder w = new StringBuilder();
				if (sc != null) for (Gizmo g : members(sc)) if (g instanceof Window) w.append(g.getClass().getName()).append(' ');
				log("SCENE " + (sc == null ? "null" : sc.getClass().getSimpleName()) + " windows=[" + w + "]");
			}
		});
		watchdog.setDaemon(true);
		watchdog.start();
		HeroClass cls;
		try {
			cls = HeroClass.valueOf(System.getProperty("spd.autotour").toUpperCase());
		} catch (Exception e){
			cls = Random.element(HeroClass.values());
		}
		log("START " + cls);
		GamesInProgress.selectedClass = cls;
		GamesInProgress.curSlot = GamesInProgress.firstEmpty();
		if (GamesInProgress.curSlot == -1) GamesInProgress.curSlot = 1;
		Dungeon.hero = null;
		Dungeon.daily = Dungeon.dailyReplay = false;
		Dungeon.initSeed();
		ActionIndicator.clearAction();
		InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
		Game.switchScene(InterlevelScene.class);
	}

	//called every frame from GameScene.update, on the render thread
	public static void update( GameScene scene ){
		if (!enabled() || Dungeon.hero == null || Dungeon.level == null) return;

		if (Dungeon.depth != lastDepth){
			lastDepth = Dungeon.depth;
			levelTime = 0;
			log("DEPTH " + Dungeon.depth + " " + Dungeon.level.getClass().getSimpleName()
					+ " caves=" + Dungeon.cavesPath + " city=" + Dungeon.cityPath);
			if (Dungeon.depth >= Dungeon.LAST_DEPTH){
				log("COMPLETE");
				Game.instance.finish();
				return;
			}
		}
		levelTime += Game.elapsed;
		statusTimer += Game.elapsed;
		if (statusTimer > 5f){
			statusTimer = 0;
			int bossCount = 0;
			for (Mob m : Dungeon.level.mobs) if (Char.hasProp(m, Char.Property.BOSS)) bossCount++;
			int windows = 0;
			for (Gizmo g : members(scene)) if (g instanceof Window) windows++;
			log("STATUS depth=" + Dungeon.depth + " t=" + Math.round(levelTime) + " ready=" + Dungeon.hero.ready
					+ " locked=" + Dungeon.level.locked + " bosses=" + bossCount + " windows=" + windows
					+ " curAction=" + Dungeon.hero.curAction + " actorThread=" + com.shatteredpixel.shatteredpixeldungeon.actors.Actor.processing());
		}

		//keep the hero alive, they are only a camera here
		if (Dungeon.hero.isAlive()) Dungeon.hero.HP = Dungeon.hero.HT;

		//answer any open window, choosing randomly between the options
		for (Gizmo g : members(scene)){
			if (g instanceof Window && ((Window) g).active){
				idle += Game.elapsed;
				if (idle < 0.4f) return;
				idle = 0;
				Window w = (Window) g;
				if (w instanceof WndOptions){
					int choice = Random.Int(2);
					log("WINDOW " + w.getClass().getName() + " choose " + choice);
					w.hide();
					try {
						Method m = WndOptions.class.getDeclaredMethod("onSelect", int.class);
						m.setAccessible(true);
						m.invoke(w, choice);
					} catch (Exception e){
						throw new RuntimeException(e);
					}
				} else {
					log("WINDOW " + w.getClass().getName() + " closed");
					w.hide();
				}
				return;
			}
		}

		if (!Dungeon.hero.ready) return;
		idle += Game.elapsed;
		if (idle < 0.3f) return;
		idle = 0;

		//fight bosses by repeatedly damaging them, which lets their phases play out
		ArrayList<Mob> bosses = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m.alignment == Char.Alignment.ENEMY && m.isAlive()
					&& (Char.hasProp(m, Char.Property.BOSS) || Char.hasProp(m, Char.Property.BOSS_MINION))){
				bosses.add(m);
			}
		}
		if (!bosses.isEmpty() && levelTime < 25f){
			for (Mob m : bosses){
				if (m.sprite != null && !m.sprite.visible) m.beckon(Dungeon.hero.pos);
				m.damage(Math.max(1, m.HT/4), Dungeon.hero);
			}
			return;
		}
		if (!bosses.isEmpty()){
			log("BOSS STUCK, killing directly: " + bosses);
			for (Mob m : bosses) if (m.isAlive()) m.die(Dungeon.hero);
			return;
		}

		if (Dungeon.level.locked){
			if (levelTime > 30f){
				log("LEVEL STILL LOCKED, unsealing");
				Dungeon.level.unseal();
			}
			return;
		}

		LevelTransition exit = null;
		for (LevelTransition t : Dungeon.level.transitions){
			if (t.type == LevelTransition.Type.REGULAR_EXIT) exit = t;
		}
		if (exit == null){
			if (levelTime > 30f){
				log("NO EXIT on " + Dungeon.level.getClass().getSimpleName() + ", falling instead");
				InterlevelScene.mode = InterlevelScene.Mode.FALL;
				Game.switchScene(InterlevelScene.class);
			}
			return;
		}
		Dungeon.hero.pos = exit.cell();
		Dungeon.level.activateTransition(Dungeon.hero, exit);
	}

	static void press( com.shatteredpixel.shatteredpixeldungeon.ui.Button button ){
		try {
			Method m = com.shatteredpixel.shatteredpixeldungeon.ui.Button.class.getDeclaredMethod("onClick");
			m.setAccessible(true);
			m.invoke(button);
			log("PRESSED " + button.getClass().getName());
		} catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	private static ArrayList<Gizmo> members( Group g ){
		try {
			Field f = Group.class.getDeclaredField("members");
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			ArrayList<Gizmo> list = (ArrayList<Gizmo>) f.get(g);
			return new ArrayList<>(list);
		} catch (Exception e){
			return new ArrayList<>();
		}
	}
}
