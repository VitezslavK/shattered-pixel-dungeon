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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.Effects;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.TengusMask;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.ChainLink;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GuardSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Alternative prison boss. Drags the hero in with chains, cripples with shackles,
// and calls the prison guards when hurt.
public class Jailer extends Mob {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 240 : 200;
		EXP = 20;
		defenseSkill = 14;
		spriteClass = JailerSprite.class;

		properties.add(Property.BOSS);
		properties.add(Property.UNDEAD);

		HUNTING = new Hunting();
	}

	private int chainCooldown = 3;
	private int guardCallsLeft = 2;

	private boolean enraged(){
		return HP*2 <= HT;
	}

	@Override
	public int damageRoll() {
		return enraged() ? Random.NormalIntRange(8, 17) : Random.NormalIntRange(6, 14);
	}

	@Override
	public int attackSkill( Char target ) {
		return 22;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 6);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		//shackles
		if (Random.Int(4) == 0) {
			Buff.prolong(enemy, Cripple.class, 3f);
		}
		return damage;
	}

	@Override
	protected boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}
		if (chainCooldown > 0) chainCooldown--;
		return super.act();
	}

	private boolean chain( int target ){
		if (enemy.properties().contains(Property.IMMOVABLE)) return false;

		Ballistica chain = new Ballistica(pos, target, Ballistica.PROJECTILE);

		if (chain.collisionPos != enemy.pos
				|| chain.path.size() < 2
				|| Dungeon.level.pit[chain.path.get(1)]) {
			return false;
		}

		int newPos = -1;
		for (int i : chain.subPath(1, chain.dist)){
			if (!Dungeon.level.solid[i] && Actor.findChar(i) == null
					&& (Dungeon.level.openSpace[i] || !Char.hasProp(enemy, Property.LARGE))){
				newPos = i;
				break;
			}
		}
		if (newPos == -1) return false;

		final int newPosFinal = newPos;
		final Char pulled = enemy;
		this.target = newPos;
		chainCooldown = enraged() ? 5 : 8;

		if (sprite.visible || pulled.sprite.visible) {
			yell(Messages.get(this, "chain"));
			new Item().throwSound();
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			sprite.parent.add(new Chains(sprite.center(),
					pulled.sprite.destinationCenter(),
					Effects.Type.CHAIN,
					new Callback() {
						public void call() {
							Actor.add(new Pushing(pulled, pulled.pos, newPosFinal, new Callback() {
								public void call() {
									pullEnemy(pulled, newPosFinal);
								}
							}));
							next();
						}
					}));
		} else {
			pullEnemy(pulled, newPos);
		}
		return true;
	}

	private void pullEnemy( Char enemy, int pullPos ){
		enemy.pos = pullPos;
		enemy.sprite.place(pullPos);
		Dungeon.level.occupyCell(enemy);
		Buff.prolong(enemy, Cripple.class, 3f);
		if (enemy == Dungeon.hero) {
			Dungeon.hero.interrupt();
			Dungeon.observe();
			GameScene.updateFog();
		} else {
			enemy.sprite.visible = Dungeon.level.heroFOV[pullPos];
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

		if (guardCallsLeft > 0 && HP <= HT * guardCallsLeft / 3f){
			guardCallsLeft--;
			ArrayList<Mob> guards = new ArrayList<>();
			guards.add(new JailGuard());
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) guards.add(new JailGuard());
			if (ArenaBosses.summonAround(this, guards) > 0){
				yell(Messages.get(this, "guards"));
			}
		}

		ArenaBosses.afterDamage(this, dmg, src);
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		ArenaBosses.killMinions(JailGuard.class);

		ArrayList<Item> rewards = new ArrayList<>();
		if (Dungeon.hero.subClass == HeroSubClass.NONE) {
			rewards.add(new TengusMask());
		}
		rewards.add(new ChainLink().quantity(2 + Random.Int(2)));
		ArenaBosses.onDeath(this, 1, 2000, rewards);
	}

	@Override
	public void notice() {
		super.notice();
		ArenaBosses.onNotice(this);
	}

	private class Hunting extends Mob.Hunting {
		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (chainCooldown <= 0
					&& enemyInFOV
					&& !isCharmedBy( enemy )
					&& !canAttack( enemy )
					&& Dungeon.level.distance( pos, enemy.pos ) <= 6
					&& chain(enemy.pos)){
				return !(sprite.visible || enemy.sprite.visible);
			} else {
				return super.act( enemyInFOV, justAlerted );
			}
		}
	}

	private static final String CHAIN_CD = "chain_cd";
	private static final String GUARD_CALLS = "guard_calls";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CHAIN_CD, chainCooldown );
		bundle.put( GUARD_CALLS, guardCallsLeft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		chainCooldown = bundle.getInt( CHAIN_CD );
		guardCallsLeft = bundle.getInt( GUARD_CALLS );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (enraged()) BossHealthBar.bleed(true);
	}

	//guards called in by the jailer, they give no exp or loot
	public static class JailGuard extends Guard {
		{
			EXP = 0;
			maxLvl = -2;
			lootChance = 0;
		}
	}

	public static class JailerSprite extends GuardSprite {
		public JailerSprite(){
			super();
			scale.set(1.3f);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			tint(0.15f, 0.15f, 0.3f, 0.4f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}
}
