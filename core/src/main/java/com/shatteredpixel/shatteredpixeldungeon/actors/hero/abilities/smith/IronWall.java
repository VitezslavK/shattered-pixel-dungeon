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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.smith;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith.Heat;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith.Smithing;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

//Iron Wall: the Smith plants themselves behind a wall of iron, gaining heavy armor and
// throwing part of every melee blow back at the attacker
public class IronWall extends ArmorAbility {

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		float duration = 8f + 4f*hero.pointsInTalent(Talent.LONG_WALL);
		Buff.prolong(hero, Smithing.IronWall.class, duration);
		//without Walking Fortress the Smith can't move while the wall stands
		if (!hero.hasTalent(Talent.WALKING_FORTRESS)){
			Buff.prolong(hero, Roots.class, duration);
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		hero.sprite.centerEmitter().burst(Speck.factory(Speck.FORGE), 6);
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1, 0.7f);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.IRON_WALL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.THORNED_IRON, Talent.LONG_WALL, Talent.WALKING_FORTRESS, Talent.HEROIC_ENERGY};
	}
}
