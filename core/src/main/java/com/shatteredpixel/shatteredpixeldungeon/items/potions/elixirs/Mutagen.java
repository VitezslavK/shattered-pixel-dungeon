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

package com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

//Distilled by the alchemist in their field cauldron. Drinking it causes a random, permanent mutation.
public class Mutagen extends Elixir {

	{
		image = ItemSpriteSheet.MUTAGEN;
	}

	@Override
	public void apply( Hero hero ) {
		Mutations.Mutation m = Mutations.mutate(hero);
		if (m == null){
			GLog.w(Messages.get(this, "no_more"));
			return;
		}
		hero.sprite.emitter().burst(PoisonParticle.SPLASH, 12);
		GLog.p(Messages.get(this, "mutated", m.title(), m.desc()));
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null){
			desc += "\n\n" + Messages.get(this, "desc_count",
					Mutations.count(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero), Mutations.MAX_MUTATIONS);
		}
		return desc;
	}

	@Override
	public int value() {
		return 60 * quantity;
	}

	@Override
	public int energyVal() {
		return 6 * quantity;
	}
}
