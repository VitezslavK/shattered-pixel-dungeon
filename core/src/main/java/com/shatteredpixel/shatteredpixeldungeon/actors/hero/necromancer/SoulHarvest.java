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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//the soul reaper's stacking buff, gained from kills and consumed souls
public class SoulHarvest extends Buff {

	{
		type = buffType.POSITIVE;
	}

	public static final int MAX_STACKS = 5;
	private static final float DECAY_DELAY = 15f;
	private static final float DECAY_RATE = 8f;

	private int stacks = 0;
	private float decay = DECAY_DELAY;

	public static void gain( Hero hero, int amount ){
		SoulHarvest buff = Buff.affect(hero, SoulHarvest.class);
		buff.stacks = Math.min(MAX_STACKS, buff.stacks + amount);
		buff.decay = DECAY_DELAY;
		BuffIndicator.refreshHero();
	}

	public static int stacks( Hero hero ){
		SoulHarvest buff = hero.buff(SoulHarvest.class);
		return buff == null ? 0 : buff.stacks;
	}

	@Override
	public boolean act() {
		decay -= TICK;
		if (decay <= 0){
			stacks--;
			decay = DECAY_RATE;
			if (stacks <= 0){
				detach();
				return true;
			}
			BuffIndicator.refreshHero();
		}
		spend(TICK);
		return true;
	}

	//+6% melee damage per stack
	public float damageFactor(){
		return 1f + 0.06f*stacks;
	}

	//3% lifesteal per stack
	public float lifestealFactor(){
		return 0.03f*stacks;
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.3f, 1f, 0.5f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - stacks/(float)MAX_STACKS;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", stacks, Math.round(100*(damageFactor()-1f)), Math.round(100*lifestealFactor()));
	}

	private static final String STACKS = "stacks";
	private static final String DECAY = "decay";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
		bundle.put(DECAY, decay);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
		decay = bundle.getFloat(DECAY);
	}
}
