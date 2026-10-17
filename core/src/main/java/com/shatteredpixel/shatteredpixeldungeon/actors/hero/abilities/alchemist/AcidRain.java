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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.alchemist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

//rains corrosive acid onto an area
public class AcidRain extends ArmorAbility {

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	public int targetedPos(Char user, int dst) {
		return dst;
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;
		if (!Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_vision"));
			return;
		}

		// radius 2, +1 at +2 and +4
		int radius = 2 + hero.pointsInTalent(Talent.CAUSTIC_STORM)/2;
		// corrosion strength scales with level, +25% per point of corroding power
		int strength = Math.round((2 + hero.lvl/3f) * (1f + 0.25f*hero.pointsInTalent(Talent.CORRODING_POWER)));

		PathFinder.buildDistanceMap( target, BArray.not( Dungeon.level.solid, null ), radius );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				Splash.at( i, 0x88CC22, 4);
				Char ch = Actor.findChar(i);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY){
					Buff.affect(ch, Corrosion.class).set(5f, strength);
					if (hero.hasTalent(Talent.BLINDING_MIST)){
						Buff.prolong(ch, Blindness.class, 2*hero.pointsInTalent(Talent.BLINDING_MIST));
					}
				}
			}
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		Sample.INSTANCE.play(Assets.Sounds.SHATTER);
		hero.sprite.zap(target);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.ACID_RAIN;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.CAUSTIC_STORM, Talent.CORRODING_POWER, Talent.BLINDING_MIST, Talent.HEROIC_ENERGY};
	}
}
