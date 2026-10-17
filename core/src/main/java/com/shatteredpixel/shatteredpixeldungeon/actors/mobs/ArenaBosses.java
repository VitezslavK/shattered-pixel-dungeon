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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//shared logic for the alternative bosses that fight in arena levels, see ArenaLevels
public class ArenaBosses {

	public static void onNotice( Mob boss ){
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(boss);
			Dungeon.level.seal();
			boss.yell(Messages.get(boss, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	public static void beforeDamage( Mob boss ){
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss( boss );
			Dungeon.level.seal();
		}
	}

	public static void afterDamage( Mob boss, int dmg, Object src ){
		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !boss.isImmune(src.getClass()) && !boss.isInvulnerable(src.getClass())){
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.addTime(dmg);
			else                                                    lock.addTime(dmg*1.5f);
		}
	}

	//call after super.die()
	public static void onDeath( Mob boss, int scoreIdx, int score, ArrayList<Item> rewards ){
		Dungeon.level.unseal();
		GameScene.bossSlain();

		Dungeon.level.drop( new WornKey( Dungeon.depth ), boss.pos ).sprite.drop();
		for (Item i : rewards){
			int ofs;
			int tries = 20;
			do {
				ofs = PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[boss.pos + ofs] && --tries > 0);
			if (tries <= 0) ofs = 0;
			Dungeon.level.drop( i, boss.pos + ofs ).sprite.drop( boss.pos );
		}

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[scoreIdx] += score;

		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) {
			beacon.upgrade();
		}

		boss.yell( Messages.get(boss, "defeated") );
	}

	//summons minions in free cells adjacent to the boss
	public static int summonAround( Mob boss, ArrayList<Mob> minions ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = boss.pos + i;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}
		Random.shuffle(cells);
		int summoned = 0;
		for (Mob m : minions){
			if (summoned >= cells.size()) break;
			m.pos = cells.get(summoned++);
			m.state = m.HUNTING;
			GameScene.add(m, 1f);
		}
		return summoned;
	}

	public static void killMinions( Class<? extends Mob> cls ){
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (cls.isInstance(m)){
				m.die(null);
			}
		}
	}

	public static int count( Class<? extends Mob> cls ){
		int count = 0;
		for (Mob m : Dungeon.level.mobs){
			if (cls.isInstance(m)) count++;
		}
		return count;
	}
}
