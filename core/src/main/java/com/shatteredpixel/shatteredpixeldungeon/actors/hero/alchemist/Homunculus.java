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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ModMobFrames;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.SlimeSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.LinkedHashSet;

//The alchemist's living creation. It is grown in the field cauldron and fed potions: every new
// kind of potion gives it a lasting trait, strength and experience potions make it bigger.
public class Homunculus extends DirectableAlly {

	{
		spriteClass = HomunculusSprite.class;
		HP = HT = 15;
		immunities.add(AllyBuff.class);
		properties.add(Property.INORGANIC);
	}

	public static final int MAX_TRAITS = 4;
	public static final int MAX_GROWTH = 6;

	private LinkedHashSet<WeaponCoating.Kind> traits = new LinkedHashSet<>();
	public int growth = 0;
	private float healTimer = 0;

	public Homunculus(){
		super();
		updateStats();
		HP = HT;
	}

	private int heroLevel(){
		return Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
	}

	public boolean hasTrait( WeaponCoating.Kind k ){
		return traits.contains(k);
	}

	public void updateStats(){
		int newHT = Math.round((10 + 4*heroLevel()) * (1f + 0.2f*growth));
		if (newHT > HT) HP += newHT - HT;
		HT = newHT;
		HP = Math.min(HP, HT);
		defenseSkill = heroLevel() + 5;
		if (hasTrait(WeaponCoating.Kind.SHADOW)) defenseSkill = Math.round(defenseSkill * 1.5f);
		flying = hasTrait(WeaponCoating.Kind.GALE);
		baseSpeed = hasTrait(WeaponCoating.Kind.HASTE) ? 1.5f : 1f;
		immunities.remove(Burning.class);
		immunities.remove(Chill.class);
		immunities.remove(Frost.class);
		immunities.remove(ToxicGas.class);
		immunities.remove(Poison.class);
		immunities.remove(ParalyticGas.class);
		immunities.remove(CorrosiveGas.class);
		if (hasTrait(WeaponCoating.Kind.FLAME)) immunities.add(Burning.class);
		if (hasTrait(WeaponCoating.Kind.FROST)) { immunities.add(Chill.class); immunities.add(Frost.class); }
		if (hasTrait(WeaponCoating.Kind.TOXIC)) { immunities.add(ToxicGas.class); immunities.add(Poison.class); }
		if (hasTrait(WeaponCoating.Kind.PURITY)) {
			immunities.add(ToxicGas.class); immunities.add(ParalyticGas.class); immunities.add(CorrosiveGas.class);
		}
	}

	//returns the message to show, or null if the potion can't be fed
	public String feed( Potion potion ){
		if (potion instanceof PotionOfStrength || potion instanceof PotionOfExperience){
			if (growth >= MAX_GROWTH) return Messages.get(Homunculus.class, "fed_full");
			growth = Math.min(MAX_GROWTH, growth + (potion instanceof PotionOfExperience ? 2 : 1));
			updateStats();
			HP = HT;
			return Messages.get(Homunculus.class, "fed_growth", growth);
		}
		WeaponCoating.Kind k = WeaponCoating.Kind.of(potion);
		if (k == null) return null;
		if (k == WeaponCoating.Kind.HEALING || traits.contains(k)){
			HP = HT;
			if (!traits.contains(k) && traits.size() < MAX_TRAITS){
				traits.add(k);
				updateStats();
				return Messages.get(Homunculus.class, "fed_trait", k.title());
			}
			return Messages.get(Homunculus.class, "fed_heal");
		}
		if (traits.size() >= MAX_TRAITS){
			HP = HT;
			return Messages.get(Homunculus.class, "fed_no_room");
		}
		traits.add(k);
		updateStats();
		HP = HT;
		return Messages.get(Homunculus.class, "fed_trait", k.title());
	}

	@Override
	protected boolean act() {
		updateStats();
		if (hasTrait(WeaponCoating.Kind.HEALING)){
			healTimer += 1f;
			if (healTimer >= 3f){
				healTimer = 0;
				if (HP < HT) HP++;
				Char hero = Dungeon.hero;
				if (hero != null && hero.isAlive() && hero.HP < hero.HT
						&& Dungeon.level.adjacent(pos, hero.pos)){
					hero.HP++;
					if (hero.sprite != null) hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, "1", FloatingText.HEALING);
				}
			}
		}
		int oldPos = pos;
		boolean result = super.act();
		if ((pos == target || oldPos == pos) && sprite != null && sprite.looping()){
			sprite.idle();
		}
		return result;
	}

	@Override
	public int attackSkill(Char target) {
		int acc = heroLevel() + 8;
		if (hasTrait(WeaponCoating.Kind.INSIGHT)) acc = Math.round(acc * 1.5f);
		return acc;
	}

	@Override
	public int damageRoll() {
		int lvl = heroLevel();
		int dmg = Random.NormalIntRange(1 + lvl/4, 3 + Math.round(lvl*0.7f));
		return Math.round(dmg * (1f + 0.15f*growth));
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1 + heroLevel()/4 + growth/2);
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (!enemy.isAlive()) return damage;
		if (hasTrait(WeaponCoating.Kind.FLAME) && Random.Int(3) == 0 && !enemy.isImmune(Burning.class)){
			Buff.affect(enemy, Burning.class).reignite(enemy, 3f);
		}
		if (hasTrait(WeaponCoating.Kind.FROST)) Buff.prolong(enemy, Chill.class, 2f);
		if (hasTrait(WeaponCoating.Kind.TOXIC)) Buff.affect(enemy, Poison.class).extend(2 + Dungeon.scalingDepth()/4f);
		if (hasTrait(WeaponCoating.Kind.PARALYTIC) && Random.Int(5) == 0) Buff.prolong(enemy, Paralysis.class, 1.5f);
		return damage;
	}

	@Override
	public String description() {
		String desc = super.description();
		if (!traits.isEmpty()){
			StringBuilder t = new StringBuilder();
			for (WeaponCoating.Kind k : traits){
				t.append("\n_").append(k.title()).append(":_ ").append(Messages.get(Homunculus.class, "trait_" + k.name().toLowerCase()));
			}
			desc += "\n\n" + Messages.get(Homunculus.class, "desc_traits", traits.size(), MAX_TRAITS) + t;
		} else {
			desc += "\n\n" + Messages.get(Homunculus.class, "desc_no_traits");
		}
		if (growth > 0) desc += "\n\n" + Messages.get(Homunculus.class, "desc_growth", growth, MAX_GROWTH);
		return desc;
	}

	@Override
	public void defendPos(int cell) {
		GLog.i(Messages.get(Homunculus.class, "direct_defend"));
		super.defendPos(cell);
	}

	@Override
	public void followHero() {
		GLog.i(Messages.get(Homunculus.class, "direct_follow"));
		super.followHero();
	}

	@Override
	public void targetChar(Char ch) {
		GLog.i(Messages.get(Homunculus.class, "direct_attack"));
		super.targetChar(ch);
	}

	public static Homunculus get(){
		if (Dungeon.level == null) return null;
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof Homunculus && m.isAlive()) return (Homunculus) m;
		}
		return null;
	}

	private static final String TRAITS = "traits";
	private static final String GROWTH = "growth";
	private static final String HEAL_TIMER = "heal_timer";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		String[] names = new String[traits.size()];
		int i = 0;
		for (WeaponCoating.Kind k : traits) names[i++] = k.name();
		bundle.put(TRAITS, names);
		bundle.put(GROWTH, growth);
		bundle.put(HEAL_TIMER, healTimer);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		traits.clear();
		for (String n : bundle.getStringArray(TRAITS)){
			try { traits.add(WeaponCoating.Kind.valueOf(n)); } catch (IllegalArgumentException e){ /* removed trait */ }
		}
		growth = bundle.getInt(GROWTH);
		healTimer = bundle.getFloat(HEAL_TIMER);
		updateStats();
	}

	public static class HomunculusSprite extends SlimeSprite {
		public HomunculusSprite(){
			super();
			ModMobFrames.apply( this, "sprites/mod/homunculus.png", 13, 12, 3, 12 );
		}
	}
}
