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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SoulLantern;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SkeletonSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the necromancer's summoned ally. Its stats scale with hero level and soul lantern level.
public class SkeletalServant extends DirectableAlly {

	{
		spriteClass = ServantSprite.class;

		HP = HT = 12;

		immunities.add(AllyBuff.class);

		properties.add(Property.UNDEAD);
		properties.add(Property.INORGANIC);
	}

	//used by the deathless servant talent
	protected boolean revived = false;

	//permanent ranks from the lantern's empower soul art, each one makes the servant stronger
	public static final int MAX_RANK = 3;
	public int rank = 0;

	public boolean empower(){
		HP = HT;
		if (rank < MAX_RANK){
			rank++;
			updateStats();
			HP = HT;
			return true;
		}
		return false;
	}

	public SkeletalServant(){
		super();
		updateStats();
		HP = HT;
	}

	protected static int lanternLevel(){
		if (Dungeon.hero == null) return 0;
		SoulLantern lantern = Dungeon.hero.belongings.getItem(SoulLantern.class);
		return lantern == null ? 0 : lantern.level();
	}

	protected int heroLevel(){
		return Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
	}

	public void updateStats(){
		//weaker than the hero on its own: it is a support, not a second hero
		int newHT = Math.round((6 + 3*heroLevel() + 2*lanternLevel()) * Evolution.servantHealthFactor() * (1f + 0.15f*rank));
		if (newHT > HT){
			HP += newHT - HT;
		}
		HT = newHT;
		HP = Math.min(HP, HT);
		defenseSkill = heroLevel() + 2;
	}

	@Override
	protected boolean act() {
		updateStats();
		int oldPos = pos;
		boolean result = super.act();
		//partially simulates how the hero switches to idle animation
		if ((pos == target || oldPos == pos) && sprite != null && sprite.looping()){
			sprite.idle();
		}
		return result;
	}

	@Override
	public int attackSkill(Char target) {
		return Math.round((heroLevel() + 6) * Evolution.servantAccuracyFactor());
	}

	@Override
	public int damageRoll() {
		int lvl = heroLevel();
		int lantern = lanternLevel();
		int dmg = Random.NormalIntRange(1 + lvl/5 + lantern/4, 2 + lvl/2 + lantern/3);
		return Math.round(dmg * (1f + 0.1f*rank));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, heroLevel()/5 + rank) + Evolution.servantArmor();
	}

	@Override
	public float speed() {
		float speed = super.speed();
		//moves 2 tiles at a time when returning to the hero
		if (state == WANDERING
				&& defendingPos == -1
				&& Dungeon.level.distance(pos, Dungeon.hero.pos) > 1){
			speed *= 2;
		}
		return speed;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (Dungeon.hero.subClass == HeroSubClass.BONELORD
				&& Dungeon.hero.hasTalent(Talent.COMMANDING_PRESENCE)
				&& Dungeon.level.distance(pos, Dungeon.hero.pos) <= 3){
			// +10/20/30% damage when close to the bonelord
			damage = Math.round(damage * (1f + 0.1f*Dungeon.hero.pointsInTalent(Talent.COMMANDING_PRESENCE)));
		}
		return damage;
	}

	@Override
	public void die(Object cause) {
		if (!revived && Dungeon.hero.hasTalent(Talent.DEATHLESS_SERVANT)){
			revived = true;
			HP = Math.max(1, Math.round(HT * 0.15f * Dungeon.hero.pointsInTalent(Talent.DEATHLESS_SERVANT)));
			if (sprite != null) {
				sprite.emitter().burst(ShadowParticle.UP, 8);
				sprite.showStatus(0x00FF66, Messages.get(this, "reformed"));
			}
			Sample.INSTANCE.play(Assets.Sounds.BONES);
			return;
		}

		//risen skeletons never trigger bone explosion
		if (!(this instanceof RisenSkeleton)
				&& Dungeon.hero.subClass == HeroSubClass.BONELORD && Dungeon.hero.hasTalent(Talent.BONE_EXPLOSION)){
			explode(Math.round(HT * 0.15f * Dungeon.hero.pointsInTalent(Talent.BONE_EXPLOSION)));
		}

		super.die(cause);
	}

	//used by corpse explosion, the servant is destroyed and cannot reform
	public void detonate( int damage ){
		revived = true;
		explode(damage);
		die(null);
	}

	//deals damage to all adjacent enemies, used by bone explosion and corpse explosion
	public void explode( int damage ){
		if (damage <= 0) return;
		CellEmitter.center(pos).burst(Speck.factory(Speck.BONE), 12);
		Sample.INSTANCE.play(Assets.Sounds.BONES);
		ArrayList<Char> targets = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(pos + i);
			if (ch != null && ch.alignment == Alignment.ENEMY){
				targets.add(ch);
			}
		}
		for (Char ch : targets){
			ch.damage(damage, this);
		}
	}

	@Override
	public void defendPos(int cell) {
		GLog.i(Messages.get(SkeletalServant.class, "direct_defend"));
		super.defendPos(cell);
	}

	@Override
	public void followHero() {
		GLog.i(Messages.get(SkeletalServant.class, "direct_follow"));
		super.followHero();
	}

	@Override
	public void targetChar(Char ch) {
		GLog.i(Messages.get(SkeletalServant.class, "direct_attack"));
		super.targetChar(ch);
	}

	public static ArrayList<SkeletalServant> getServants(){
		ArrayList<SkeletalServant> servants = new ArrayList<>();
		if (Dungeon.level == null) return servants;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof SkeletalServant && !(m instanceof RisenSkeleton) && m.isAlive()){
				servants.add((SkeletalServant) m);
			}
		}
		return servants;
	}

	@Override
	public String description() {
		String desc = super.description();
		if (rank > 0 && !(this instanceof RisenSkeleton)){
			desc += "\n\n" + Messages.get(SkeletalServant.class, "desc_rank", rank, MAX_RANK);
		}
		return desc;
	}

	private static final String REVIVED = "revived";
	private static final String RANK = "rank";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(REVIVED, revived);
		bundle.put(RANK, rank);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		revived = bundle.getBoolean(REVIVED);
		rank = bundle.getInt(RANK);
	}

	//temporary skeletons, from army of the dead and cracked lanterns
	public static class RisenSkeleton extends SkeletalServant {

		{
			spriteClass = RisenSprite.class;
			intelligentAlly = false;
			revived = true; //can never reform
		}

		private float timeRemaining = 20f;

		public RisenSkeleton(){
			super();
		}

		public RisenSkeleton( float duration ){
			super();
			timeRemaining = duration;
		}

		@Override
		protected boolean act() {
			if (timeRemaining <= 0){
				die(null);
				return true;
			}
			return super.act();
		}

		@Override
		protected void spend(float time) {
			super.spend(time);
			timeRemaining -= time;
		}

		@Override
		public void updateStats() {
			int newHT = Math.round(0.75f * (8 + 4*heroLevel()));
			if (newHT > HT){
				HP += newHT - HT;
			}
			HT = newHT;
			HP = Math.min(HP, HT);
			defenseSkill = heroLevel() + 4;
		}

		@Override
		public int attackSkill(Char target) {
			// +10% accuracy per point of risen fury
			return Math.round(super.attackSkill(target) * (1f + 0.1f*Dungeon.hero.pointsInTalent(Talent.RISEN_FURY)));
		}

		@Override
		public int damageRoll() {
			int lvl = heroLevel();
			int dmg = Random.NormalIntRange(1 + lvl/4, 2 + Math.round(lvl*0.6f));
			// +10% damage per point of risen fury
			return Math.round(dmg * (1f + 0.1f*Dungeon.hero.pointsInTalent(Talent.RISEN_FURY)));
		}

		@Override
		public int attackProc(Char enemy, int damage) {
			damage = super.attackProc(enemy, damage);
			if (Dungeon.hero.hasTalent(Talent.GRAVE_CHILL) && enemy.isAlive()){
				Buff.prolong(enemy, Chill.class, Dungeon.hero.pointsInTalent(Talent.GRAVE_CHILL));
			}
			return damage;
		}

		@Override
		public String description() {
			String message = super.description();
			if (Actor.chars().contains(this)){
				message += "\n\n" + Messages.get(this, "desc_remaining", (int)Math.ceil(timeRemaining));
			}
			return message;
		}

		private static final String TIME_REMAINING  = "time_remaining";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TIME_REMAINING, timeRemaining);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			timeRemaining = bundle.getFloat(TIME_REMAINING);
		}
	}

	public static class ServantSprite extends SkeletonSprite {

		@Override
		public void resetColor() {
			super.resetColor();
			tint(0.3f, 1f, 0.5f, 0.25f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}

	public static class RisenSprite extends SkeletonSprite {

		@Override
		public void resetColor() {
			super.resetColor();
			alpha(0.8f);
			tint(0.5f, 0.3f, 1f, 0.35f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}

}
