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
package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

//From a lucky coin landing heads: enemies drop loot more often and gold piles are bigger.
public class Fortune extends FlavourBuff {

	public static final float DURATION = 200f;

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.85f, 0.25f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	public static float lootMultiplier( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ){
		return ch != null && ch.buff(Fortune.class) != null ? 1.5f : 1f;
	}

	public static float goldMultiplier( com.shatteredpixel.shatteredpixeldungeon.actors.Char ch ){
		return ch != null && ch.buff(Fortune.class) != null ? 1.25f : 1f;
	}
}
