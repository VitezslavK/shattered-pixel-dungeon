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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Sold by the shopkeeper, sealed and rattling. It might hold junk, a fine piece of gear,
// a golden treasure, or (the shopkeeper's favourite joke) a handful of the buyer's own coins.
public class MysteryBox extends Item {

	private static final String AC_OPEN = "OPEN";

	{
		image = ItemSpriteSheet.MYSTERY_BOX;
		defaultAction = AC_OPEN;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_OPEN);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_OPEN)){
			detach(hero.belongings.backpack);
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
			if (hero.sprite != null) hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 6);

			ArrayList<Item> found = new ArrayList<>();
			int roll = Random.Int(100);
			String msg;
			if (roll < 40){
				//a bundle of consumables
				for (int i = 0; i < 3; i++){
					found.add(Generator.random(Random.oneOf(Generator.Category.POTION, Generator.Category.SCROLL, Generator.Category.STONE)));
				}
				msg = "supplies";
			} else if (roll < 70){
				//a piece of gear, often improved
				Item gear = Generator.random(Random.oneOf(Generator.Category.WEAPON, Generator.Category.ARMOR,
						Generator.Category.RING, Generator.Category.WAND));
				gear.cursed = false;
				if (Random.Int(2) == 0) gear.upgrade();
				found.add(gear.identify(false));
				msg = "gear";
			} else if (roll < 90){
				found.add(GoldItems.random(Random.oneOf(GoldItems.Kind.values()), Dungeon.depth));
				msg = "golden";
			} else {
				found.add(new Gold(Random.IntRange(10, 25) * (1 + Dungeon.depth / 5)));
				msg = "joke";
			}
			GLog.p(Messages.get(this, msg));
			for (Item i : found){
				if (!i.doPickUp(hero)){
					com.shatteredpixel.shatteredpixeldungeon.items.Heap h = Dungeon.level.drop(i, hero.pos);
					if (h.sprite != null) h.sprite.drop();
				}
			}
			hero.spendAndNext(1f);
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
		return 45 * quantity;
	}
}
