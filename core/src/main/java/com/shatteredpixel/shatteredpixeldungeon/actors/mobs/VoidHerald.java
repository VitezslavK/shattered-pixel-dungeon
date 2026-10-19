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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.VoidShard;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WarlockSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//The final boss of the abyss (floor 30), guarding the way to the Amulet of Yendor.
// Phase 1: summons void walkers. Phase 2: also blinks away from melee.
// Phase 3: also collapses the ceiling around the hero, which must be dodged.
public class VoidHerald extends Warlock {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 1000 : 800;
		EXP = 60;
		maxLvl = Hero.MAX_LEVEL;
		defenseSkill = 34;
		spriteClass = HeraldSprite.class;
		lootChance = 0;

		properties.add(Property.BOSS);
		properties.add(Property.DEMONIC);
	}

	private int summonCooldown = 6;
	private int blinkCooldown = 0;
	private int collapseCooldown = 4;

	private int phase(){
		if (HP*3 > HT*2) return 1;
		if (HP*3 > HT) return 2;
		return 3;
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(30, 45); }

	@Override
	public int attackSkill( Char target ) { return 46; }

	@Override
	public int drRoll() { return Random.NormalIntRange(0, 16); }

	@Override
	protected boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		if (state == HUNTING){
			if (--summonCooldown <= 0){
				summonCooldown = phase() == 1 ? 12 : 16;
				if (ArenaBosses.count(VoidWalker.class) < 3){
					ArrayList<Mob> walkers = new ArrayList<>();
					walkers.add(new HeraldWalker());
					if (phase() == 1) walkers.add(new HeraldWalker());
					if (ArenaBosses.summonAround(this, walkers) > 0){
						yell(Messages.get(this, "summon"));
					}
				}
			}

			if (blinkCooldown > 0) blinkCooldown--;
			if (phase() >= 2 && enemy != null && blinkCooldown <= 0
					&& Dungeon.level.adjacent(pos, enemy.pos) && blink()){
				return true;
			}

			if (phase() == 3 && enemy != null && --collapseCooldown <= 0){
				collapseCooldown = 5;
				collapse(enemy.pos);
			}
		}

		return super.act();
	}

	private boolean blink(){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			int dist = Dungeon.level.distance(i, enemy.pos);
			if (dist >= 3 && dist <= 6 && Dungeon.level.passable[i] && !Dungeon.level.avoid[i]
					&& Actor.findChar(i) == null
					&& new Ballistica(i, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos){
				candidates.add(i);
			}
		}
		if (candidates.isEmpty()) return false;
		ScrollOfTeleportation.appear(this, Random.element(candidates));
		blinkCooldown = 5;
		spend(1f);
		return true;
	}

	//marks cells around the target, which take heavy damage two turns later
	private void collapse( int center ){
		ArrayList<Integer> cells = new ArrayList<>();
		cells.add(center);
		for (int i : PathFinder.NEIGHBOURS8){
			if (Random.Int(2) == 0 && !Dungeon.level.solid[center + i]) cells.add(center + i);
		}
		Buff.append(this, VoidCollapse.class, 2f).setRockPositions(cells);
		yell(Messages.get(this, "collapse"));
	}

	public static class VoidCollapse extends DelayedRockFall {
		@Override
		public void affectChar(Char ch) {
			ch.damage(Random.NormalIntRange(30, 50), this);
			if (ch.isAlive()) Buff.prolong(ch, Vulnerable.class, 3f);
			if (ch == Dungeon.hero && !ch.isAlive()){
				Dungeon.fail(this);
				GLog.n(Messages.get(VoidHerald.class, "collapse_kill"));
			}
		}
	}

	@Override
	protected void zap() {
		spend( 1f );
		Invisibility.dispel(this);
		Char enemy = this.enemy;
		if (hit( this, enemy, true )) {
			enemy.damage( Random.NormalIntRange(24, 36) + (phase()-1)*4, new DarkBolt() );
			if (enemy == Dungeon.hero && !enemy.isAlive()) {
				Badges.validateDeathFromEnemyMagic();
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "bolt_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		ArenaBosses.beforeDamage(this);
		int oldPhase = phase();
		super.damage(dmg, src);
		if (!isAlive()) return;
		if (phase() != oldPhase){
			if (phase() == 2) BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "phase"));
			yell(Messages.get(this, "phase" + phase()));
		}
		ArenaBosses.afterDamage(this, dmg, src);
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		ArenaBosses.killMinions(VoidWalker.class);

		ArrayList<Item> rewards = new ArrayList<>();
		rewards.add(new VoidShard().quantity(2 + Random.Int(2)));
		ArenaBosses.onDeath(this, 4, 6000, rewards);
	}

	@Override
	public void notice() {
		super.notice();
		ArenaBosses.onNotice(this);
	}

	@Override
	public Item createLoot() {
		return null;
	}

	private static final String SUMMON = "summon", BLINK = "blink", COLLAPSE = "collapse";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put(SUMMON, summonCooldown);
		bundle.put(BLINK, blinkCooldown);
		bundle.put(COLLAPSE, collapseCooldown);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		summonCooldown = bundle.getInt(SUMMON);
		blinkCooldown = bundle.getInt(BLINK);
		collapseCooldown = bundle.getInt(COLLAPSE);
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (phase() >= 2) BossHealthBar.bleed(true);
	}

	//void walkers summoned by the herald give no exp or loot
	public static class HeraldWalker extends VoidWalker {
		{
			EXP = 0;
			maxLvl = -2;
			lootChance = 0;
		}
	}

	public static class HeraldSprite extends WarlockSprite {
		public HeraldSprite(){
			super();
			ModMobFrames.apply( this, "sprites/mod/void_herald.png", 22, 24, 4, 8 );
		}
	}
}
