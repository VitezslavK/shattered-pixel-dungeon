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

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WarlockSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

//abyss caster whose bolts of darkness blind their targets
public class LightEater extends Warlock {

	{
		spriteClass = EaterSprite.class;
		HP = HT = 100;
		defenseSkill = 32;
		EXP = 20;
		maxLvl = Hero.MAX_LEVEL - 1;
		lootChance = 0.25f;
		properties.add(Property.DEMONIC);
	}

	@Override
	public int damageRoll() { return Random.NormalIntRange(18, 28); }

	@Override
	public int attackSkill( Char target ) { return 44; }

	@Override
	protected void zap() {
		spend( 1f );
		Invisibility.dispel(this);
		Char enemy = this.enemy;
		if (hit( this, enemy, true )) {
			if (Random.Int(2) == 0) Buff.prolong(enemy, Blindness.class, 3f);
			enemy.damage( Random.NormalIntRange(20, 30), new DarkBolt() );
			if (enemy == Dungeon.hero && !enemy.isAlive()) {
				Badges.validateDeathFromEnemyMagic();
				Dungeon.fail( this );
				GLog.n( Messages.get(this, "bolt_kill") );
			}
		} else {
			enemy.sprite.showStatus( CharSprite.NEUTRAL, enemy.defenseVerb() );
		}
	}

	public static class EaterSprite extends WarlockSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(0f, 0f, 0f, 0.55f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}
}
