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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.arcanist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//releases a burst of arcane energy that damages every visible enemy nearby
public class ArcaneDischarge extends ArmorAbility {

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {

		// radius 2, +1 at +2 and +4
		int radius = 2 + hero.pointsInTalent(Talent.OVERLOAD_RADIUS)/2;
		// 5 + lvl damage, +12.5% per point of discharge power
		int dmg = Math.round((5 + hero.lvl) * (1f + 0.125f*hero.pointsInTalent(Talent.DISCHARGE_POWER)));

		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= radius){
				targets.add(ch);
			}
		}
		for (Char ch : targets){
			ch.damage(dmg, this);
			CellEmitter.center(ch.pos).burst(EnergyParticle.FACTORY, 10);
			if (ch.isAlive() && hero.hasTalent(Talent.STATIC_FIELD)){
				Buff.prolong(ch, Vertigo.class, 2*hero.pointsInTalent(Talent.STATIC_FIELD));
			}
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(EnergyParticle.FACTORY, 20);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.ARCANE_DISCHARGE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.OVERLOAD_RADIUS, Talent.DISCHARGE_POWER, Talent.STATIC_FIELD, Talent.HEROIC_ENERGY};
	}
}
