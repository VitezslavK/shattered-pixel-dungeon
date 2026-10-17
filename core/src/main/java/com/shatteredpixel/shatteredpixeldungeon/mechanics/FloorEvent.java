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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

//Random events that change how a floor plays, rolled when the floor is generated.
public enum FloorEvent {
	NONE,
	BLOOD_MOON,     //tougher monsters that give more exp, extra loot
	MIST,           //reduced vision
	GOLD_RUSH,      //extra gold scattered around
	TREASURE_GOBLIN,//a fleeing goblin carrying treasure
	QUIET;          //far fewer monsters

	public String title(){
		return Messages.get(FloorEvent.class, name().toLowerCase() + "_title");
	}

	public String desc(){
		return Messages.get(FloorEvent.class, name().toLowerCase() + "_desc");
	}

	//roughly 1 in 4 floors gets an event
	public static FloorEvent roll(){
		switch (Random.Int(20)){
			case 0: case 1:     return BLOOD_MOON;
			case 2:             return MIST;
			case 3:             return GOLD_RUSH;
			case 4:             return TREASURE_GOBLIN;
			case 5:             return QUIET;
			default:            return NONE;
		}
	}
}
