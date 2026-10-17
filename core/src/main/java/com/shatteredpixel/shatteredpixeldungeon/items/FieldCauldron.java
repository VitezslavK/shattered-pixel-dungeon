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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist.Alchemy;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//The alchemist's class item. Lets them brew anywhere, slowly generates alchemical energy,
// and lets the transmuter transmute items for energy.
public class FieldCauldron extends Item {

	public static final String AC_BREW       = "BREW";
	public static final String AC_TRANSMUTE  = "TRANSMUTE";

	public static final int BASE_TRANSMUTE_COST = 5;

	{
		image = ItemSpriteSheet.FIELD_CAULDRON;
		defaultAction = AC_BREW;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add(AC_BREW);
		if (hero.subClass == HeroSubClass.TRANSMUTER){
			actions.add(AC_TRANSMUTE);
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_BREW)){
			if (Dungeon.level.locked){
				GLog.w(Messages.get(this, "locked"));
				return;
			}
			AlchemyScene.clearToolkit();
			Game.switchScene(AlchemyScene.class);
		} else if (action.equals(AC_TRANSMUTE)){
			if (Dungeon.energy < transmuteCost(hero)){
				GLog.w(Messages.get(this, "no_energy", transmuteCost(hero)));
			} else {
				GameScene.selectItem(new Transmuter());
			}
		}
	}

	public static int transmuteCost( Hero hero ){
		return Alchemy.transmuteCost(hero);
	}

	//keeps the cauldron's passive energy generation going
	public static void ensureBuff( Hero hero ){
		if (hero.buff(Alchemy.CauldronEnergy.class) == null){
			Buff.affect(hero, Alchemy.CauldronEnergy.class);
		}
	}

	@Override
	public boolean collect( com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag container ) {
		if (super.collect(container)){
			if (container.owner instanceof Hero) ensureBuff((Hero) container.owner);
			return true;
		}
		return false;
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.TRANSMUTER){
			desc += "\n\n" + Messages.get(this, "desc_transmute", transmuteCost(Dungeon.hero));
		}
		return desc;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	//reuses the scroll of transmutation's rules for which items can be transmuted
	private static TransmuteCheck transmuteCheck;
	private static boolean canTransmute( Item item ){
		if (transmuteCheck == null) transmuteCheck = new TransmuteCheck();
		return transmuteCheck.canTransmute(item);
	}
	private static class TransmuteCheck extends ScrollOfTransmutation {
		public boolean canTransmute( Item item ){
			return usableOnItem(item);
		}
	}

	private class Transmuter extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(FieldCauldron.class, "prompt_transmute");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return !item.unique && !item.isEquipped(Dungeon.hero) && canTransmute(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			Hero hero = Dungeon.hero;
			int cost = transmuteCost(hero);
			if (Dungeon.energy < cost) return;
			Item result = ScrollOfTransmutation.changeItem(item);
			if (result == null) return;

			Alchemy.onTransmute(result);

			Dungeon.energy -= cost;
			item.detach(hero.belongings.backpack);
			if (!result.collect()){
				Dungeon.level.drop(result, hero.pos).sprite.drop();
			}
			Catalog.countUse(FieldCauldron.class);
			GLog.p(Messages.get(FieldCauldron.class, "transmuted", item.name(), result.name()));
			hero.sprite.emitter().start(Speck.factory(Speck.CHANGE), 0.2f, 10);
			Sample.INSTANCE.play(Assets.Sounds.EVOKE);
			hero.sprite.operate(hero.pos);
			hero.spendAndNext(Actor.TICK);
			updateQuickslot();
		}
	}
}
