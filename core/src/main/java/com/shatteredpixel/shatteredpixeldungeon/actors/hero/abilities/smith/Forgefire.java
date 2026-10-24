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

//Forge Fire: the Smith stokes their weapon to full heat in an instant, scorching everything around them
public class Forgefire extends ArmorAbility {

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		int radius = 1 + hero.pointsInTalent(Talent.WIDE_BLAST)/2;
		int dmg = Math.round((5 + hero.lvl) * (1f + 0.2f*hero.pointsInTalent(Talent.SEARING_BLAST)));

		PathFinder.buildDistanceMap(hero.pos, BArray.not(Dungeon.level.solid, null), radius);
		for (int cell = 0; cell < PathFinder.distance.length; cell++){
			if (PathFinder.distance[cell] == Integer.MAX_VALUE || cell == hero.pos) continue;
			Char ch = Actor.findChar(cell);
			if (ch != null && ch.alignment != Char.Alignment.ALLY){
				ch.damage(dmg, this);
				if (ch.isAlive() && !ch.isImmune(Burning.class)) Buff.affect(ch, Burning.class).reignite(ch);
			}
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(FlameParticle.FACTORY, 6);
		}

		Smithing.heat(hero).set(Heat.MAX);
		if (hero.hasTalent(Talent.BELLOWS)){
			Buff.affect(hero, Barrier.class).incShield(4*hero.pointsInTalent(Talent.BELLOWS));
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.7f, 1.3f);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.FORGEFIRE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.WIDE_BLAST, Talent.SEARING_BLAST, Talent.BELLOWS, Talent.HEROIC_ENERGY};
	}
}
