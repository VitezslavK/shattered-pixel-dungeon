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
package com.shatteredpixel.shatteredpixeldungeon.items.gold;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Fortune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Flip it: heads brings fortune (more loot, bigger gold piles), tails a blessing.
public class LuckyCoin extends Item {

	private static final String AC_FLIP = "FLIP";

	{
		image = ItemSpriteSheet.LUCKY_COIN;
		stackable = true;
		defaultAction = AC_FLIP;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_FLIP);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_FLIP)){
			detach(hero.belongings.backpack);
			Sample.INSTANCE.play(Assets.Sounds.GOLD, 1, 1, 1.3f);
			if (hero.sprite != null) hero.sprite.emitter().burst(Speck.factory(Speck.COIN), 5);
			if (Random.Int(2) == 0){
				Buff.affect(hero, Fortune.class, Fortune.DURATION);
				GLog.p(Messages.get(this, "heads"));
			} else {
				Buff.affect(hero, Bless.class, Bless.DURATION);
				GLog.p(Messages.get(this, "tails"));
			}
			hero.spendAndNext(1f);
			updateQuickslot();
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 35 * quantity;
	}

	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe.SimpleRecipe {
		{
			inputs =  new Class[]{GoldIngot.class};
			inQuantity = new int[]{1};
			cost = 2;
			output = LuckyCoin.class;
			outQuantity = 2;
		}
	}
}
