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

package com.shatteredpixel.shatteredpixeldungeon.levels.puzzles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

//A harmless, always visible plate that drives a puzzle when stepped on. It doesn't break when
// used, so it can be pressed again (sequence puzzles, re-opening timed doors).
public class PressurePlate extends Trap {

	{
		color = GREY;
		shape = LARGE_DOT;

		canBeHidden = false;
		canBeSearched = false;
		disarmedByActivation = false;
		avoidsHallways = true;
		visible = true;
	}

	public PressurePlate setColor( int color ){
		this.color = color;
		return this;
	}

	@Override
	public void trigger() {
		if (active) {
			Dungeon.level.discover(pos);
			activate();
		}
	}

	@Override
	public void activate() {
		Sample.INSTANCE.play(Assets.Sounds.CLICK);
		Puzzle puzzle = Puzzle.forPlate(Dungeon.level, pos);
		if (puzzle != null) puzzle.onPlate(pos);
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + color);
	}

	private static final String COLOR = "color";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(COLOR, color);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		color = bundle.getInt(COLOR);
	}
}
