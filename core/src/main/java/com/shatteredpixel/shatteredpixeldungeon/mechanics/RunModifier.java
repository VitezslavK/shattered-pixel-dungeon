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

package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

//An optional twist picked at the start of a run, each with an upside and a downside.
public enum RunModifier {
	GLASS_CANNON,   //+25% damage, -25% max HP
	HOARDER,        //+1 item per floor, -30% gold
	IRONCLAD,       //+1-3 armor, -10% speed
	SCHOLAR,        //+20% exp, monsters +15% HP
	SWIFT;          //+15% speed, -15% max HP

	public String title(){
		return Messages.get(RunModifier.class, name().toLowerCase() + "_title");
	}

	public String desc(){
		return Messages.get(RunModifier.class, name().toLowerCase() + "_desc");
	}

	public static boolean active( RunModifier m ){
		return Dungeon.runModifier == m.ordinal();
	}

	public static float damageFactor(){
		return active(GLASS_CANNON) ? 1.25f : 1f;
	}

	public static float hpFactor(){
		if (active(GLASS_CANNON)) return 0.75f;
		if (active(SWIFT)) return 0.85f;
		return 1f;
	}

	public static float speedFactor(){
		if (active(IRONCLAD)) return 0.9f;
		if (active(SWIFT)) return 1.15f;
		return 1f;
	}

	public static float goldFactor(){
		return active(HOARDER) ? 0.7f : 1f;
	}

	public static float expFactor(){
		return active(SCHOLAR) ? 1.2f : 1f;
	}

	public static float mobHPFactor(){
		return active(SCHOLAR) ? 1.15f : 1f;
	}
}
