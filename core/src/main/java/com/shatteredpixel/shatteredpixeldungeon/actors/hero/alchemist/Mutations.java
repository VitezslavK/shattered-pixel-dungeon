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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashSet;

//Permanent changes the alchemist gains by drinking mutagens. Each run allows a few of them.
public class Mutations extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public enum Mutation {
		THICK_HIDE,    //+armor
		VIGOR,         //+max HP
		REGROWTH,      //slow natural healing
		TOXIC_BLOOD,   //immune to poison and harmful gases
		FEROCITY,      //+melee damage
		ADRENAL;       //burst of speed when badly hurt

		public String title(){
			return Messages.get(Mutations.class, name().toLowerCase() + "_title");
		}

		public String desc(){
			return Messages.get(Mutations.class, name().toLowerCase() + "_desc");
		}
	}

	public static final int MAX_MUTATIONS = 3;

	private LinkedHashSet<Mutation> owned = new LinkedHashSet<>();
	private float regrowth = 0;
	private float adrenalCooldown = 0;

	public static boolean has( Char ch, Mutation m ){
		Mutations muts = ch.buff(Mutations.class);
		return muts != null && muts.owned.contains(m);
	}

	public static int count( Char ch ){
		Mutations muts = ch.buff(Mutations.class);
		return muts == null ? 0 : muts.owned.size();
	}

	//adds a random mutation the hero doesn't have yet, returns null if they can't mutate further
	public static Mutation mutate( Hero hero ){
		Mutations muts = Buff.affect(hero, Mutations.class);
		if (muts.owned.size() >= MAX_MUTATIONS) return null;
		ArrayList<Mutation> options = new ArrayList<>();
		for (Mutation m : Mutation.values()){
			if (!muts.owned.contains(m)) options.add(m);
		}
		if (options.isEmpty()) return null;
		Mutation m = Random.element(options);
		muts.owned.add(m);
		if (m == Mutation.VIGOR) hero.updateHT(true);
		BuffIndicator.refreshHero();
		return m;
	}

	public static float htFactor( Hero hero ){
		return has(hero, Mutation.VIGOR) ? 1.15f : 1f;
	}

	@Override
	public boolean act() {
		if (owned.contains(Mutation.REGROWTH) && target.HP < target.HT && Regeneration.regenOn()){
			regrowth += 0.1f;
			if (regrowth >= 1f){
				regrowth -= 1f;
				target.HP++;
			}
		}
		if (adrenalCooldown > 0) adrenalCooldown--;
		if (owned.contains(Mutation.TOXIC_BLOOD)){
			Buff.detach(target, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison.class);
		}
		spend(TICK);
		return true;
	}

	//called when the hero is about to take damage, with their HP after the hit
	public void onDamaged( int hpAfter ){
		if (owned.contains(Mutation.ADRENAL) && adrenalCooldown <= 0
				&& target.isAlive() && hpAfter > 0 && hpAfter < target.HT * 0.3f){
			Buff.prolong(target, Haste.class, 3f);
			adrenalCooldown = 40;
			if (target.sprite != null) target.sprite.showStatus(0x66FF66, Mutation.ADRENAL.title());
		}
	}

	@Override
	public int icon() {
		return owned.isEmpty() ? BuffIndicator.NONE : BuffIndicator.HEALING;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.5f, 1f, 0.3f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(owned.size());
	}

	@Override
	public String desc() {
		StringBuilder desc = new StringBuilder(Messages.get(this, "desc", owned.size(), MAX_MUTATIONS));
		for (Mutation m : owned){
			desc.append("\n\n_").append(m.title()).append(":_ ").append(m.desc());
		}
		return desc.toString();
	}

	private static final String OWNED = "owned";
	private static final String REGROWTH = "regrowth";
	private static final String ADRENAL = "adrenal";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		String[] names = new String[owned.size()];
		int i = 0;
		for (Mutation m : owned) names[i++] = m.name();
		bundle.put(OWNED, names);
		bundle.put(REGROWTH, regrowth);
		bundle.put(ADRENAL, adrenalCooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		owned.clear();
		for (String n : bundle.getStringArray(OWNED)){
			try { owned.add(Mutation.valueOf(n)); } catch (IllegalArgumentException e){ /* removed mutation */ }
		}
		regrowth = bundle.getFloat(REGROWTH);
		adrenalCooldown = bundle.getFloat(ADRENAL);
	}
}
