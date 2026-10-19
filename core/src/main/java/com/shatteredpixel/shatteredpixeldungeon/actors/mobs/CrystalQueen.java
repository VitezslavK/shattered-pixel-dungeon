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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.CrystalHeart;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CrystalWispSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Alternative caves boss. A huge crystal wisp that fires piercing beams of light,
// regrows a crystalline shell, and splits off smaller wisps when wounded.
public class CrystalQueen extends CrystalWisp {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 330 : 280;
		EXP = 25;
		maxLvl = Hero.MAX_LEVEL - 1;
		defenseSkill = 20;

		properties.add(Property.BOSS);
	}

	public CrystalQueen(){
		super();
		spriteClass = QueenSprite.class;
	}

	private int shellCooldown = 6;
	private boolean splitDone = false;

	private boolean enraged(){
		return HP*2 <= HT;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 8, 16 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 26;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 4);
	}

	@Override
	protected boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}
		if (state == HUNTING && --shellCooldown <= 0){
			Barrier shell = Buff.affect(this, Barrier.class);
			if (shell.shielding() < 40){
				shell.incShield(Math.min(20, 40 - shell.shielding()));
				sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "shell"));
			}
			shellCooldown = enraged() ? 8 : 12;
		}
		return super.act();
	}

	@Override
	protected boolean doAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )
				|| new Ballistica( pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos != enemy.pos) {
			return super.doAttack( enemy );
		} else if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
			sprite.zap( enemy.pos );
			return false;
		} else {
			queenZap();
			return true;
		}
	}

	@Override
	public void onZapComplete() {
		queenZap();
		next();
	}

	private void queenZap(){
		spend( 1f );
		Invisibility.dispel(this);
		Char enemy = this.enemy;
		if (hit( this, enemy, true )) {
			int dmg = Random.NormalIntRange( 8, 15 );
			if (enraged()) dmg += 3;
			enemy.damage( dmg, new LightBeam() );
			if (!enemy.isAlive() && enemy == Dungeon.hero) {
				Badges.validateDeathFromEnemyMagic();
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "beam_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );
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

		if (!splitDone && enraged()){
			splitDone = true;
			ArrayList<Mob> shards = new ArrayList<>();
			shards.add(new QueenShard());
			shards.add(new QueenShard());
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) shards.add(new QueenShard());
			if (ArenaBosses.summonAround(this, shards) > 0){
				yell(Messages.get(this, "split"));
			}
		}

		ArenaBosses.afterDamage(this, dmg, src);
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		ArenaBosses.killMinions(QueenShard.class);

		ArrayList<Item> rewards = new ArrayList<>();
		rewards.add(new CrystalHeart().quantity(2 + Random.Int(2)));
		ArenaBosses.onDeath(this, 2, 3000, rewards);
	}

	@Override
	public void notice() {
		super.notice();
		ArenaBosses.onNotice(this);
	}

	private static final String SHELL_CD = "shell_cd";
	private static final String SPLIT = "split";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SHELL_CD, shellCooldown );
		bundle.put( SPLIT, splitDone );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		spriteClass = QueenSprite.class;
		shellCooldown = bundle.getInt( SHELL_CD );
		splitDone = bundle.getBoolean( SPLIT );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (enraged()) BossHealthBar.bleed(true);
	}

	//wisps split off from the queen, they give no exp or loot
	public static class QueenShard extends CrystalWisp {
		{
			EXP = 0;
			lootChance = 0;
		}
	}

	public static class QueenSprite extends CrystalWispSprite.Red {
		public QueenSprite(){
			super();
			ModMobFrames.apply( this, "sprites/mod/crystal_queen.png", 20, 22, 4, 8 );
		}
	}
}
