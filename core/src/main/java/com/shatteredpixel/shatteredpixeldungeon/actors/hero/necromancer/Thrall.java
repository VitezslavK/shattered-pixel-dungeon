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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//an enemy whose soul was torn loose by the necromancer's enthrall soul art. It fights for the
// hero for a while, then its body gives out.
public class Thrall extends AllyBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	private float left = 20f;

	public void set( float duration ){
		left = duration;
	}

	@Override
	public boolean act() {
		left -= TICK;
		if (left <= 0){
			if (target.sprite != null) target.sprite.emitter().burst(ShadowParticle.UP, 10);
			target.die(this);
			detach();
			return true;
		}
		if (target instanceof Mob && ((Mob) target).state == ((Mob) target).SLEEPING){
			((Mob) target).state = ((Mob) target).HUNTING;
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.4f, 1f, 0.6f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int)Math.ceil(left));
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", (int)Math.ceil(left));
	}

	private static final String LEFT = "left";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(LEFT, left);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		left = bundle.getFloat(LEFT);
	}
}
