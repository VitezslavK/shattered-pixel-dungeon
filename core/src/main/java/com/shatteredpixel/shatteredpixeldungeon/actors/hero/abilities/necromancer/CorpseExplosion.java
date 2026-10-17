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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer.SkeletalServant;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SoulLantern;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//blasts an enemy with necrotic energy, which explodes outward if it dies.
// can also detonate one of the necromancer's own skeletons.
public class CorpseExplosion extends ArmorAbility {

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

		if (ch instanceof SkeletalServant){
			SkeletalServant servant = (SkeletalServant) ch;
			int blast = Math.round((servant.HP + hero.lvl) * explosionFactor(hero));
			servant.detonate(blast);
			afflict(hero, servant.pos, false);

		} else if (ch.alignment == Char.Alignment.ENEMY){
			int dmg = 6 + hero.lvl + Math.round(0.25f * (ch.HT - ch.HP));
			if (hero.hasTalent(Talent.NECROTIC_PLAGUE)){
				Buff.affect(ch, Poison.class).set(2 + 2*hero.pointsInTalent(Talent.NECROTIC_PLAGUE));
			}
			ch.damage(dmg, this);
			CellEmitter.center(target).burst(ShadowParticle.CURSE, 8);

			if (!ch.isAlive()){
				if (hero.hasTalent(Talent.CORPSE_HARVEST)){
					SoulLantern.giveSouls(hero, 0.5f*hero.pointsInTalent(Talent.CORPSE_HARVEST));
				}
				int blast = Math.round(dmg * explosionFactor(hero));
				ArrayList<Char> targets = new ArrayList<>();
				for (int i : PathFinder.NEIGHBOURS8){
					Char other = Actor.findChar(target + i);
					if (other != null && other.alignment == Char.Alignment.ENEMY){
						targets.add(other);
					}
				}
				for (Char other : targets){
					other.damage(blast, this);
				}
				afflict(hero, target, true);
			}

		} else {
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		hero.sprite.zap(target);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	// explosion is 50% of the initial damage, +12.5% per point of volatile remains
	private static float explosionFactor( Hero hero ){
		return 0.5f + 0.125f*hero.pointsInTalent(Talent.VOLATILE_REMAINS);
	}

	private static void afflict( Hero hero, int pos, boolean onlyAdjacent ){
		CellEmitter.center(pos).burst(ShadowParticle.UP, 12);
		if (!hero.hasTalent(Talent.NECROTIC_PLAGUE)) return;
		for (int i : PathFinder.NEIGHBOURS9){
			if (onlyAdjacent && i == 0) continue;
			Char other = Actor.findChar(pos + i);
			if (other != null && other.alignment == Char.Alignment.ENEMY && other.isAlive()){
				Buff.affect(other, Poison.class).set(2 + 2*hero.pointsInTalent(Talent.NECROTIC_PLAGUE));
			}
		}
	}

	@Override
	public int icon() {
		return HeroIcon.CORPSE_EXPLOSION;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.VOLATILE_REMAINS, Talent.CORPSE_HARVEST, Talent.NECROTIC_PLAGUE, Talent.HEROIC_ENERGY};
	}
}
