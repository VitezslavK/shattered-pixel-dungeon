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

import com.shatteredpixel.shatteredpixeldungeon.journal.Discoveries;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Ancient runes found on tablets throughout the dungeon. Each deciphered rune is remembered
// forever in the codex of discoveries, and grants a small permanent boon to every future run.
public class Runes {

	public enum Rune {
		FEHU,       //+10% gold
		URUZ,       //+5 max HP
		KENAZ,      //+1 vision range
		ALGIZ,      //shielding when arriving on a new floor
		BERKANO,    //+10% experience
		ANSUZ;      //one random potion type is known at the start of a run

		public String title(){
			return Messages.get(Runes.class, name().toLowerCase() + "_title");
		}

		public String desc(){
			return Messages.get(Runes.class, name().toLowerCase() + "_desc");
		}

		public boolean active(){
			return Discoveries.isDeciphered(this);
		}
	}

	public static boolean allDeciphered(){
		for (Rune r : Rune.values()){
			if (!r.active()) return false;
		}
		return true;
	}

	//deciphers a random rune the player doesn't know yet, returns null if all are known
	public static Rune decipherRandom(){
		ArrayList<Rune> unknown = new ArrayList<>();
		for (Rune r : Rune.values()){
			if (!r.active()) unknown.add(r);
		}
		if (unknown.isEmpty()) return null;
		Rune r = Random.element(unknown);
		Discoveries.decipher(r);
		return r;
	}
}
