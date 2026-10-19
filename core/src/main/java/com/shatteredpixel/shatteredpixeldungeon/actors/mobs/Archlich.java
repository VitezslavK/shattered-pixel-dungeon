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

import com.shatteredpixel.shatteredpixeldungeon.sprites.ModMobFrames;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.EssenceOfAscension;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SkeletonSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Alternative boss for floor 25. An ancient lich that drains the hero's soul from afar,
// raises skeletons, and shields itself behind its army when wounded.
public class Archlich extends Mob {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 700 : 600;
		EXP = 50;
		maxLvl = Hero.MAX_LEVEL;
		defenseSkill = 30;
		spriteClass = LichSprite.class;

		properties.add(Property.BOSS);
		properties.add(Property.UNDEAD);
	}

	private int drainCooldown = 3;
	private int raiseCooldown = 6;
	private boolean bonewallUsed = false;

	@Override
	public int damageRoll() { return Random.NormalIntRange(25, 40); }

	@Override
	public int attackSkill( Char target ) { return 44; }

	@Override
	public int drRoll() { return Random.NormalIntRange(0, 14); }

	@Override
	protected boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		if (state == HUNTING && enemy != null && enemy.isAlive()){
			if (--raiseCooldown <= 0){
				raiseCooldown = 10;
				if (ArenaBosses.count(LichSkeleton.class) < 4){
					ArrayList<Mob> skeletons = new ArrayList<>();
					skeletons.add(new LichSkeleton());
					skeletons.add(new LichSkeleton());
					if (ArenaBosses.summonAround(this, skeletons) > 0){
						yell(Messages.get(this, "raise"));
					}
				}
			}

			if (--drainCooldown <= 0 && fieldOfView != null && fieldOfView[enemy.pos]
					&& new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos){
				drainCooldown = 4;
				drain();
				spend(TICK);
				return true;
			}
		}
		return super.act();
	}

	private void drain(){
		if (sprite != null && sprite.visible){
			sprite.parent.add(new Beam.HealthRay(sprite.center(), enemy.sprite.center()));
		}
		int dmg = Random.NormalIntRange(15, 25);
		enemy.damage(dmg, this);
		int heal = Math.min(dmg, HT - HP);
		if (heal > 0){
			HP += heal;
			sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		if (enemy == Dungeon.hero && !enemy.isAlive()){
			Dungeon.fail(this);
			GLog.n(Messages.get(this, "drain_kill"));
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		ArenaBosses.beforeDamage(this);
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (!bonewallUsed && HP*2 <= HT){
			bonewallUsed = true;
			BossHealthBar.bleed(true);
			Buff.affect(this, Barrier.class).setShield(100);
			ArrayList<Mob> army = new ArrayList<>();
			for (int i = 0; i < 4; i++) army.add(new LichSkeleton());
			ArenaBosses.summonAround(this, army);
			yell(Messages.get(this, "bonewall"));
		}
		ArenaBosses.afterDamage(this, dmg, src);
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		ArenaBosses.killMinions(LichSkeleton.class);
		ArrayList<Item> rewards = new ArrayList<>();
		rewards.add(new EssenceOfAscension());
		ArenaBosses.onDeath(this, 4, 5000, rewards);
	}

	@Override
	public void notice() {
		super.notice();
		ArenaBosses.onNotice(this);
	}

	private static final String DRAIN = "drain", RAISE = "raise", WALL = "wall";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put(DRAIN, drainCooldown);
		bundle.put(RAISE, raiseCooldown);
		bundle.put(WALL, bonewallUsed);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		drainCooldown = bundle.getInt(DRAIN);
		raiseCooldown = bundle.getInt(RAISE);
		bonewallUsed = bundle.getBoolean(WALL);
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (bonewallUsed) BossHealthBar.bleed(true);
	}

	//skeletons raised by the lich give no exp or loot
	public static class LichSkeleton extends Skeleton {
		{
			EXP = 0;
			maxLvl = -2;
			lootChance = 0;
			HP = HT = 50;
		}
	}

	public static class LichSprite extends SkeletonSprite {
		public LichSprite(){
			super();
			ModMobFrames.apply( this, "sprites/mod/archlich.png", 18, 22, 3, 8 );
		}
	}
}
