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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ArcaneCore;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WarlockSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//Alternative city boss. A dwarven archmage who blinks away from melee, bombards with
// arcane bolts, conjures elementals, and animates a golem when cornered.
public class GolemArchmage extends Warlock {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 360 : 300;
		EXP = 30;
		maxLvl = Hero.MAX_LEVEL - 1;
		defenseSkill = 24;
		spriteClass = ArchmageSprite.class;

		lootChance = 0;

		properties.add(Property.BOSS);
	}

	private int blinkCooldown = 0;
	private int conjureCooldown = 8;
	private boolean golemSummoned = false;

	private boolean enraged(){
		return HP*2 <= HT;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 10, 18 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 32;
	}

	@Override
	public int drRoll() {
		return Random.NormalIntRange(0, 10);
	}

	@Override
	protected boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}
		if (blinkCooldown > 0) blinkCooldown--;

		if (state == HUNTING && --conjureCooldown <= 0){
			if (ArenaBosses.count(Elemental.class) < (enraged() ? 3 : 2)){
				ArrayList<Mob> summons = new ArrayList<>();
				summons.add(conjureElemental());
				if (ArenaBosses.summonAround(this, summons) > 0){
					yell(Messages.get(this, "conjure"));
				}
			}
			conjureCooldown = enraged() ? 10 : 15;
		}

		//blinks away from adjacent enemies
		if (state == HUNTING && enemy != null && blinkCooldown <= 0
				&& Dungeon.level.adjacent(pos, enemy.pos) && blink()){
			return true;
		}

		return super.act();
	}

	private boolean blink(){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			int dist = Dungeon.level.distance(i, enemy.pos);
			if (dist >= 4 && dist <= 7
					&& Dungeon.level.passable[i] && !Dungeon.level.avoid[i]
					&& Actor.findChar(i) == null
					&& new Ballistica(i, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos){
				candidates.add(i);
			}
		}
		if (candidates.isEmpty()) return false;

		ScrollOfTeleportation.appear(this, Random.element(candidates));
		blinkCooldown = enraged() ? 4 : 6;
		spend(1f);
		return true;
	}

	@Override
	protected void zap() {
		spend( 1f );
		Invisibility.dispel(this);
		Char enemy = this.enemy;
		if (hit( this, enemy, true )) {
			if (Random.Int(2) == 0) {
				Buff.prolong( enemy, Weakness.class, 4f );
			}
			int dmg = Random.NormalIntRange( 14, 22 );
			enemy.damage( dmg, new DarkBolt() );
			if (enemy == Dungeon.hero && !enemy.isAlive()) {
				Badges.validateDeathFromEnemyMagic();
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "bolt_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL,  enemy.defenseVerb() );
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		ArenaBosses.beforeDamage(this);
		boolean wasEnraged = enraged();
		super.damage(dmg, src);
		if (!isAlive()) return;

		if (enraged() && !wasEnraged){
			BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "enraged"));
		}

		if (!golemSummoned && HP*3 <= HT){
			golemSummoned = true;
			ArrayList<Mob> golem = new ArrayList<>();
			golem.add(new ConjuredGolem());
			if (ArenaBosses.summonAround(this, golem) > 0){
				yell(Messages.get(this, "golem"));
			}
		}

		ArenaBosses.afterDamage(this, dmg, src);
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		ArenaBosses.killMinions(Elemental.class);
		ArenaBosses.killMinions(ConjuredGolem.class);

		ArrayList<Item> rewards = new ArrayList<>();
		rewards.add(new KingsCrown());
		rewards.add(new ArcaneCore().quantity(2 + Random.Int(2)));
		ArenaBosses.onDeath(this, 3, 4000, rewards);
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

	private static final String BLINK_CD = "blink_cd";
	private static final String CONJURE_CD = "conjure_cd";
	private static final String GOLEM = "golem";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( BLINK_CD, blinkCooldown );
		bundle.put( CONJURE_CD, conjureCooldown );
		bundle.put( GOLEM, golemSummoned );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		blinkCooldown = bundle.getInt( BLINK_CD );
		conjureCooldown = bundle.getInt( CONJURE_CD );
		golemSummoned = bundle.getBoolean( GOLEM );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (enraged()) BossHealthBar.bleed(true);
	}

	//summons give no exp or loot
	private static Mob conjureElemental(){
		Mob m = Reflection.newInstance(Elemental.random());
		m.EXP = 0;
		m.maxLvl = -2;
		m.lootChance = 0;
		return m;
	}

	public static class ConjuredGolem extends Golem {
		{
			EXP = 0;
			maxLvl = -2;
			lootChance = 0;
		}
	}

	public static class ArchmageSprite extends WarlockSprite {
		public ArchmageSprite(){
			super();
			ModMobFrames.apply( this, "sprites/mod/golem_archmage.png", 20, 22, 3, 8 );
		}
	}
}
