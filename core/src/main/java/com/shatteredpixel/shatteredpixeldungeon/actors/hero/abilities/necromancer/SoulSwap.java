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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.necromancer;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//swaps places with any visible character, draining life from enemies or giving it to allies
public class SoulSwap extends ArmorAbility {

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
		if (target == null){
			return;
		}

		Char ch = Actor.findChar(target);
		if (ch == null || ch == hero || !Dungeon.level.heroFOV[target]){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		if (Char.hasProp(ch, Char.Property.IMMOVABLE) || Char.hasProp(ch, Char.Property.BOSS)){
			GLog.w(Messages.get(this, "cant_swap"));
			return;
		}

		int heroPos = hero.pos;
		int targetPos = ch.pos;

		ScrollOfTeleportation.appear(ch, heroPos);
		ScrollOfTeleportation.appear(hero, targetPos);
		Dungeon.level.occupyCell(ch);
		Dungeon.level.occupyCell(hero);

		// 5 + lvl/2 drain, +25% per point of soul theft
		int drain = Math.round((5 + hero.lvl/2f) * (1f + 0.25f*hero.pointsInTalent(Talent.SOUL_THEFT)));

		if (ch.alignment == Char.Alignment.ENEMY){
			ch.damage(drain, this);
			if (ch.isAlive() && hero.hasTalent(Talent.SWAP_SHOCK)){
				Buff.prolong(ch, Vertigo.class, 2*hero.pointsInTalent(Talent.SWAP_SHOCK));
			}
			heal(hero, drain);
		} else {
			heal(ch, drain);
		}
		if (ch.sprite != null) ch.sprite.emitter().burst(ShadowParticle.CURSE, 6);

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		Invisibility.dispel();
		if (hero.hasTalent(Talent.PHANTOM_STEP)){
			Buff.affect(hero, Invisibility.class, hero.pointsInTalent(Talent.PHANTOM_STEP));
		}

		Dungeon.observe();
		GameScene.updateFog();
		hero.spendAndNext(Actor.TICK);
	}

	private static void heal( Char ch, int amount ){
		int heal = Math.min(amount, ch.HT - ch.HP);
		if (heal > 0){
			ch.HP += heal;
			ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
	}

	@Override
	public int icon() {
		return HeroIcon.SOUL_SWAP;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SOUL_THEFT, Talent.SWAP_SHOCK, Talent.PHANTOM_STEP, Talent.HEROIC_ENERGY};
	}
}
