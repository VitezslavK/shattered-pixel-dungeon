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

package com.shatteredpixel.shatteredpixeldungeon.journal;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Reactions;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.HashSet;

//the discovery codex: mechanics the player has found by themselves, kept across all runs
public class Discoveries {

	private static final HashSet<String> discovered = new HashSet<>();

	public static boolean isDiscovered( Reactions.Reaction r ){
		return discovered.contains(r.name());
	}

	public static int count(){
		int count = 0;
		for (Reactions.Reaction r : Reactions.Reaction.values()){
			if (isDiscovered(r)) count++;
		}
		return count;
	}

	public static void discover( Reactions.Reaction r ){
		if (discovered.add(r.name())){
			GLog.h(Messages.get(Discoveries.class, "new_discovery", r.title()));
			Sample.INSTANCE.play(Assets.Sounds.SECRET);
			Journal.saveNeeded = true;
			Journal.saveGlobal();
		}
	}

	public static boolean isDeciphered( com.shatteredpixel.shatteredpixeldungeon.mechanics.Runes.Rune r ){
		return discovered.contains("RUNE_" + r.name());
	}

	public static int runeCount(){
		int count = 0;
		for (com.shatteredpixel.shatteredpixeldungeon.mechanics.Runes.Rune r : com.shatteredpixel.shatteredpixeldungeon.mechanics.Runes.Rune.values()){
			if (isDeciphered(r)) count++;
		}
		return count;
	}

	public static void decipher( com.shatteredpixel.shatteredpixeldungeon.mechanics.Runes.Rune r ){
		if (discovered.add("RUNE_" + r.name())){
			GLog.h(Messages.get(Discoveries.class, "new_rune", r.title()));
			Sample.INSTANCE.play(Assets.Sounds.SECRET);
			Journal.saveNeeded = true;
			Journal.saveGlobal();
		}
	}

	private static final String DISCOVERIES = "discoveries";

	public static void store( Bundle bundle ){
		bundle.put(DISCOVERIES, discovered.toArray(new String[0]));
	}

	public static void restore( Bundle bundle ){
		discovered.clear();
		if (bundle.contains(DISCOVERIES)){
			for (String s : bundle.getStringArray(DISCOVERIES)){
				discovered.add(s);
			}
		}
	}
}
