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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CrabSprite;
import com.watabou.utils.Random;

//a slow cave crawler covered in a thick crystal shell
public class CrystalCrawler extends Mob {

	{
		spriteClass = CrawlerSprite.class;
		HP = HT = 50;
		defenseSkill = 12;
		baseSpeed = 0.8f;
		EXP = 9;
		maxLvl = 18;
		properties.add(Property.INORGANIC);
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(5, 12); }

	@Override
	public int attackSkill( Char target ) { return 20; }

	@Override
	public int drRoll() { return super.drRoll() + Random.NormalIntRange(2, 10); }

	public static class CrawlerSprite extends CrabSprite {
		@Override
		public void resetColor() { super.resetColor(); tint(0.4f, 0.9f, 1f, 0.5f); }
		@Override
		public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
