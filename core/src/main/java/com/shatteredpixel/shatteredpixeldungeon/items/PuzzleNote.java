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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.PuzzleRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

//A scrap of paper with the order of a sequence puzzle's coloured plates, found elsewhere on the floor.
public class PuzzleNote extends Item {

	{
		image = ItemSpriteSheet.TORN_PAGE;
		unique = true;
		bones = false;
	}

	public int[] order = new int[0];

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String desc() {
		StringBuilder colors = new StringBuilder();
		for (int i = 0; i < order.length; i++){
			if (i > 0) colors.append(" > ");
			int color = PuzzleRoom.SEQUENCE_COLORS[order[i]];
			colors.append("_").append(Messages.get(PuzzleNote.class, "color_" + color)).append("_");
		}
		return Messages.get(this, "desc", colors.toString());
	}

	@Override
	public int value() {
		return 0;
	}

	private static final String ORDER = "order";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle(bundle);
		bundle.put(ORDER, order);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle(bundle);
		order = bundle.getIntArray(ORDER);
	}
}
