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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//flying abyss monster that mends the wounds of nearby monsters
public class FallenAngel extends Mob {

	{
		spriteClass = AngelSprite.class;
		HP = HT = 110;
		defenseSkill = 34;
		EXP = 22;
		maxLvl = Hero.MAX_LEVEL - 1;
		flying = true;
	}

	private int healCooldown = 4;

	@Override
	public int damageRoll() { return Random.NormalIntRange(20, 32); }

	@Override
	public int attackSkill( Char target ) { return 42; }

	@Override
	public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 10); }

	@Override
	protected boolean act() {
		if (state == HUNTING && --healCooldown <= 0){
			healCooldown = 4;
			for (Mob m : Dungeon.level.mobs){
				if (m != this && m.alignment == alignment && m.HP < m.HT
						&& Dungeon.level.distance(pos, m.pos) <= 3){
					int heal = Math.min(15, m.HT - m.HP);
					m.HP += heal;
					if (m.sprite != null && m.sprite.visible){
						m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
						m.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
					}
				}
			}
		}
		return super.act();
	}

	@Override
	public void die(Object cause) {
		flying = false;
		super.die(cause);
	}

	private static final String HEAL_CD = "heal_cd";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HEAL_CD, healCooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		healCooldown = bundle.getInt(HEAL_CD);
	}

	public static class AngelSprite extends GhostSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(1f, 0.8f, 0.3f, 0.4f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}
}
