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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

//conjures a mirrored shell of force that shields the arcanist and reflects damage
public class MirrorBarrier extends ArmorAbility {

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {

		// 10 + lvl shielding, +25% per point of mirror shell
		int shield = Math.round((10 + hero.lvl) * (1f + 0.25f*hero.pointsInTalent(Talent.MIRROR_SHELL)));
		Buff.affect(hero, Barrier.class).incShield(shield);
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		Buff.affect(hero, MirrorTracker.class, 10f);

		if (hero.hasTalent(Talent.ARCANE_RECOIL)){
			int dmg = 2*hero.pointsInTalent(Talent.ARCANE_RECOIL) + hero.lvl/4;
			for (int i : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(hero.pos + i);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY){
					ch.damage(dmg, this);
				}
			}
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(EnergyParticle.FACTORY, 15);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		hero.spendAndNext(Actor.TICK);
	}

	//reflects a portion of damage while active, see Hero.defenseProc
	public static class MirrorTracker extends FlavourBuff {
		{ type = buffType.POSITIVE; }

		public int onHit( Hero hero, Char attacker, int damage ){
			int pts = hero.pointsInTalent(Talent.REFLECTION);
			if (pts > 0 && attacker != null && attacker.isAlive() && damage > 0){
				attacker.damage(Math.max(1, Math.round(damage * 0.1f * pts)), this);
			}
			return damage;
		}

		public int icon() { return BuffIndicator.ARMOR; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0.8f, 1f); }
		public float iconFadePercent() { return Math.max(0, (10f - visualcooldown()) / 10f); }

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	@Override
	public int icon() {
		return HeroIcon.MIRROR_BARRIER;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.MIRROR_SHELL, Talent.REFLECTION, Talent.ARCANE_RECOIL, Talent.HEROIC_ENERGY};
	}
}
