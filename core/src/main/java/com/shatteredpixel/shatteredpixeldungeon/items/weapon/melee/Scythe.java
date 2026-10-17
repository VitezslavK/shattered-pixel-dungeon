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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PathFinder;

//tier 3 weapon whose wide swings also catch another nearby enemy
public class Scythe extends MeleeWeapon {

	{
		image = ItemSpriteSheet.SCYTHE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;
		tier = 3;
		ACC = 0.9f;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//sweeps through one other enemy next to the wielder for half damage
		for (int i : PathFinder.NEIGHBOURS8){
			Char other = Actor.findChar(attacker.pos + i);
			if (other != null && other != defender && other.alignment != attacker.alignment
					&& other.alignment != Char.Alignment.NEUTRAL && Dungeon.level.adjacent(attacker.pos, other.pos)){
				other.damage(Math.max(1, damage/2), attacker);
				break;
			}
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void duelistAbility(Hero hero, Integer target) {
		int bleedAmt = augment.damageFactor(Math.round(18f + 3f*buffedLvl()));
		Sickle.harvestAbility(hero, target, 0f, bleedAmt, this);
	}

	@Override
	public String abilityInfo() {
		int bleedAmt = levelKnown ? Math.round(18f + 3f*buffedLvl()) : 18;
		if (levelKnown){
			return Messages.get(this, "ability_desc", augment.damageFactor(bleedAmt));
		} else {
			return Messages.get(this, "typical_ability_desc", bleedAmt);
		}
	}

	@Override
	public String upgradeAbilityStat(int level) {
		return Integer.toString(augment.damageFactor(Math.round(18f + 3f*level)));
	}
}
