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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer.SkeletalServant;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//raises several temporary skeletons around the necromancer
public class ArmyOfTheDead extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {

		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = hero.pos + i;
			if (Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}

		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		// 2 skeletons, +1 at +2 and +4 endless legion
		int count = 2 + hero.pointsInTalent(Talent.ENDLESS_LEGION)/2;
		// 20 turns, +2.5 per point of endless legion
		float duration = 20f + 2.5f*hero.pointsInTalent(Talent.ENDLESS_LEGION);

		Random.shuffle(cells);
		for (int i = 0; i < count && i < cells.size(); i++){
			SkeletalServant.RisenSkeleton skeleton = new SkeletalServant.RisenSkeleton(duration);
			skeleton.pos = cells.get(i);
			GameScene.add(skeleton);
			ScrollOfTeleportation.appear(skeleton, skeleton.pos);
			skeleton.sprite.emitter().burst(ShadowParticle.UP, 6);
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		Sample.INSTANCE.play(Assets.Sounds.BONES);
		hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public int icon() {
		return HeroIcon.ARMY_OF_THE_DEAD;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.ENDLESS_LEGION, Talent.RISEN_FURY, Talent.GRAVE_CHILL, Talent.HEROIC_ENERGY};
	}
}
