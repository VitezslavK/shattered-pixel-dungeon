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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Plague;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.PlagueGland;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.RatSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//An alternative boss for the sewers. Commands a brood of rats, burrows through water
// to escape when badly hurt, and spreads plague with her bite.
public class RatMatriarch extends Mob {

	{
		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 130 : 110;
		EXP = 10;
		defenseSkill = 9;
		spriteClass = MatriarchSprite.class;

		properties.add(Property.BOSS);
	}

	private static final int MAX_BROOD = 3;

	private int summonCooldown = 6;
	//HP thresholds at which she burrows away, 2/3 and 1/3 health
	private int burrowsLeft = 2;

	private boolean enraged(){
		return HP*2 <= HT;
	}

	@Override
	public int damageRoll() {
		return enraged() ? Random.NormalIntRange(2, 11) : Random.NormalIntRange(1, 8);
	}

	@Override
	public int attackSkill( Char target ) {
		return enraged() ? 14 : 11;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	@Override
	public float speed() {
		return super.speed() * (enraged() ? 1.25f : 1f);
	}

	@Override
	public int attackProc( Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );
		//plague bite
		if (Random.Int(3) == 0) {
			Buff.affect(enemy, Plague.class).set(4);
		}
		return damage;
	}

	@Override
	protected boolean act() {
		if (state != SLEEPING){
			Dungeon.level.seal();
		}

		if (state == HUNTING && --summonCooldown <= 0){
			if (broodSize() < MAX_BROOD + (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 1 : 0)){
				if (summonBrood(1)){
					yell(Messages.get(this, "summon"));
				}
			}
			summonCooldown = enraged() ? 5 : 8;
		}

		return super.act();
	}

	private int broodSize(){
		int count = 0;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof BroodRat) count++;
		}
		return count;
	}

	private boolean summonBrood( int amount ){
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = pos + i;
			if (Dungeon.level.passable[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}
		Random.shuffle(cells);
		boolean summoned = false;
		for (int i = 0; i < amount && i < cells.size(); i++){
			BroodRat rat = new BroodRat();
			rat.pos = cells.get(i);
			rat.state = rat.HUNTING;
			GameScene.add(rat, 1f);
			CellEmitter.get(rat.pos).burst(Speck.factory(Speck.DUST), 4);
			summoned = true;
		}
		return summoned;
	}

	//dives into water elsewhere in the arena, leaving some rats behind
	private void burrow(){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			if (Dungeon.level.water[i] && Actor.findChar(i) == null
					&& Dungeon.level.distance(i, Dungeon.hero.pos) >= 4
					&& Dungeon.level.distance(i, pos) <= 8){
				candidates.add(i);
			}
		}
		if (candidates.isEmpty()) return;

		summonBrood(2);
		int dest = Random.element(candidates);
		CellEmitter.get(pos).burst(Speck.factory(Speck.STEAM), 8);
		sprite.showStatus(CharSprite.WARNING, Messages.get(this, "burrow"));
		Sample.INSTANCE.play(Assets.Sounds.WATER);
		move(dest, false);
		sprite.interruptMotion();
		sprite.place(dest);
		Dungeon.level.occupyCell(this);
		CellEmitter.get(dest).burst(Speck.factory(Speck.STEAM), 8);
		aggro(Dungeon.hero);
	}

	@Override
	public void damage(int dmg, Object src) {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss( this );
			Dungeon.level.seal();
		}
		boolean wasEnraged = enraged();
		super.damage(dmg, src);
		if (!isAlive()) return;

		if (enraged() && !wasEnraged){
			BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "enraged"));
			yell(Messages.get(this, "enraged_yell"));
		}

		if (burrowsLeft > 0 && HP <= HT * burrowsLeft / 3f){
			burrowsLeft--;
			burrow();
		}

		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.addTime(dmg);
			else                                                    lock.addTime(dmg*1.5f);
		}
	}

	@Override
	public void die( Object cause ) {

		super.die( cause );

		//the brood flees in panic
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
			if (m instanceof BroodRat){
				m.die(null);
			}
		}

		Dungeon.level.unseal();

		GameScene.bossSlain();
		Dungeon.level.drop( new WornKey( Dungeon.depth ), pos ).sprite.drop();

		//2-3 plague glands
		int glands = 2 + Random.Int(2);
		for (int i = 0; i < glands; i++){
			int ofs;
			do {
				ofs = PathFinder.NEIGHBOURS8[Random.Int(8)];
			} while (!Dungeon.level.passable[pos + ofs]);
			Dungeon.level.drop( new PlagueGland(), pos + ofs ).sprite.drop( pos );
		}

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[0] += 1000;

		yell( Messages.get(this, "defeated") );
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			Dungeon.level.seal();
			yell(Messages.get(this, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	private static final String SUMMON_CD = "summon_cd";
	private static final String BURROWS   = "burrows";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( SUMMON_CD, summonCooldown );
		bundle.put( BURROWS, burrowsLeft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		summonCooldown = bundle.getInt( SUMMON_CD );
		burrowsLeft = bundle.getInt( BURROWS );
		if (state != SLEEPING) BossHealthBar.assignBoss(this);
		if (enraged()) BossHealthBar.bleed(true);
	}

	//rats summoned by the matriarch, they give no exp or loot
	public static class BroodRat extends Rat {
		{
			EXP = 0;
			maxLvl = -2;
			lootChance = 0;
			spriteClass = BroodSprite.class;
		}
	}

	public static class MatriarchSprite extends RatSprite {
		public MatriarchSprite(){
			super();
			scale.set(1.35f);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			tint(0.6f, 0.1f, 0.3f, 0.35f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}

	public static class BroodSprite extends RatSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(0.4f, 0.3f, 0.2f, 0.25f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}
}
