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

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.AmuletGuardian;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Ghost;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Imp;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Wandmaker;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ArcaneCore;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ChainLink;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CrystalHeart;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.PlagueGland;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.EndingScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene;
import com.watabou.noosa.Game;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.io.IOException;
import java.util.ArrayList;

//The different ways a run can end. Which endings are available depends on what the hero did.
public class Endings {

	public enum Ending {
		ESCAPE,     //leave with the amulet, the classic ending
		LIBERATOR,  //leave with the amulet after helping every quest NPC
		DESTROYER,  //destroy the amulet and defeat its guardian
		DARK_LORD,  //claim the amulet's power for yourself
		KEEPER;     //forge the key of the depths, the secret ending

		public String title(){
			return Messages.get(Endings.class, name().toLowerCase() + "_title");
		}

		public String text(){
			return Messages.get(Endings.class, name().toLowerCase() + "_text");
		}
	}

	public static boolean liberatorAvailable(){
		return Ghost.Quest.completed()
				&& Wandmaker.Quest.completed()
				&& Blacksmith.Quest.completed()
				&& Imp.Quest.isCompleted();
	}

	//an evolved hero carrying materials from at least 3 of the alternative bosses
	public static boolean keeperAvailable( Hero hero ){
		if (hero == null || !hero.evolved) return false;
		int materials = 0;
		if (hero.belongings.getItem(PlagueGland.class) != null) materials++;
		if (hero.belongings.getItem(ChainLink.class) != null) materials++;
		if (hero.belongings.getItem(CrystalHeart.class) != null) materials++;
		if (hero.belongings.getItem(ArcaneCore.class) != null) materials++;
		return materials >= 3;
	}

	//ends the run with the given ending and shows its epilogue
	public static void finish( Ending ending ){
		Dungeon.win( Amulet.class );
		Dungeon.deleteGame( GamesInProgress.curSlot, true );
		Badges.saveGlobal();
		EndingScene.ending = ending;
		Game.switchScene( EndingScene.class );
	}

	//destroying the amulet awakens its guardian, which the hero must defeat
	public static void startDestruction( Hero hero ){
		Amulet amulet = hero.belongings.getItem(Amulet.class);
		if (amulet != null) amulet.detachAll(hero.belongings.backpack);

		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			int dist = Dungeon.level.distance(i, hero.pos);
			if (dist >= 3 && dist <= 6 && Dungeon.level.passable[i]
					&& Actor.findChar(i) == null && Dungeon.level.findMob(i) == null){
				cells.add(i);
			}
		}
		int pos;
		if (cells.isEmpty()){
			pos = hero.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
		} else {
			pos = Random.element(cells);
		}
		AmuletGuardian guardian = new AmuletGuardian();
		guardian.pos = pos;
		Dungeon.level.mobs.add(guardian);

		try {
			Dungeon.saveAll();
		} catch (IOException e) {
			ShatteredPixelDungeon.reportException(e);
		}
		InterlevelScene.mode = InterlevelScene.Mode.CONTINUE;
		Game.switchScene(InterlevelScene.class);
	}
}
