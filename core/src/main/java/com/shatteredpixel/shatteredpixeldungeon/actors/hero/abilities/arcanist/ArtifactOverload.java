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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.ArcaneCodex;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;

//overcharges every magical item the arcanist carries
public class ArtifactOverload extends ArmorAbility {

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {

		ArtifactRecharge.chargeArtifacts(hero, 40f);
		for (Item i : hero.belongings){
			if (i instanceof Wand){
				((Wand) i).gainCharge(1f, true);
			}
		}
		if (hero.hasTalent(Talent.LINGERING_POWER)){
			Buff.prolong(hero, Recharging.class, 5*hero.pointsInTalent(Talent.LINGERING_POWER));
		}
		if (hero.hasTalent(Talent.OVERFLOW)){
			ArcaneCodex.giveEssence(hero, 2*hero.pointsInTalent(Talent.OVERFLOW));
		}
		if (hero.hasTalent(Talent.ARCANE_HASTE)){
			Buff.prolong(hero, Haste.class, hero.pointsInTalent(Talent.ARCANE_HASTE));
		}
		Item.updateQuickslot();

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		ScrollOfRecharging.charge(hero);
		SpellSprite.show(hero, SpellSprite.CHARGE);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.ARTIFACT_OVERLOAD;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.OVERFLOW, Talent.LINGERING_POWER, Talent.ARCANE_HASTE, Talent.HEROIC_ENERGY};
	}
}
