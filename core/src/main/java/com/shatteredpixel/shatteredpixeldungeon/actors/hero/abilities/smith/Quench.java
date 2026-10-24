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

//Quench: the Smith dumps all the heat at once, turning it into a protective shell and a blinding cloud of steam
public class Quench extends ArmorAbility {

	{
		baseChargeUse = 25;
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		Heat h = Smithing.heat(hero);
		float heat = h.heat();

		int shield = Math.round((5 + heat/4f) * (1f + 0.25f*hero.pointsInTalent(Talent.COLD_SHIELD)));
		Buff.affect(hero, Barrier.class).incShield(shield);

		float blind = 3 + 2*hero.pointsInTalent(Talent.STEAM_CLOUD);
		PathFinder.buildDistanceMap(hero.pos, BArray.not(Dungeon.level.solid, null), 2);
		for (int cell = 0; cell < PathFinder.distance.length; cell++){
			if (PathFinder.distance[cell] == Integer.MAX_VALUE) continue;
			Char ch = Actor.findChar(cell);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY){
				Buff.prolong(ch, Blindness.class, blind);
			}
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(Speck.factory(Speck.STEAM), 4);
		}

		if (hero.hasTalent(Talent.SOOTHING_STEAM)){
			Smithing.heal(hero, Math.round(hero.HT * 0.05f * hero.pointsInTalent(Talent.SOOTHING_STEAM)));
		}
		h.set(0);
		Buff.prolong(hero, Smithing.Tempered.class, 10f);

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.8f, 1.6f);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.QUENCH;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.STEAM_CLOUD, Talent.COLD_SHIELD, Talent.SOOTHING_STEAM, Talent.HEROIC_ENERGY};
	}
}
