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

import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.watabou.utils.Random;
import com.watabou.utils.PathFinder;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.PotionBandolier;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Mutagen;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist.WeaponCoating;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist.Mutations;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist.Homunculus;
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
	public static final String AC_HOMUNCULUS = "HOMUNCULUS";
	public static final String AC_FEED       = "FEED";
	public static final String AC_COMMAND    = "COMMAND";
	public static final String AC_MUTAGEN    = "MUTAGEN";

	public static final int HOMUNCULUS_COST = 10;
	public static final int MUTAGEN_COST    = 8;

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
		if (Homunculus.get() == null){
			actions.add(AC_HOMUNCULUS);
		} else {
			actions.add(AC_FEED);
			actions.add(AC_COMMAND);
		}
		actions.add(AC_MUTAGEN);
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
		} else if (action.equals(AC_HOMUNCULUS)){
			createHomunculus(hero);
		} else if (action.equals(AC_FEED)){
			if (Homunculus.get() == null){
				GLog.w(Messages.get(this, "no_homunculus"));
			} else {
				GameScene.selectItem(new Feeder());
			}
		} else if (action.equals(AC_COMMAND)){
			if (Homunculus.get() == null){
				GLog.w(Messages.get(this, "no_homunculus"));
			} else {
				GameScene.selectCell(new CellSelector.Listener() {
					@Override
					public void onSelect(Integer cell) {
						Homunculus h = Homunculus.get();
						if (cell == null || h == null) return;
						h.directTocell(cell);
					}

					@Override
					public String prompt() {
						return Messages.get(FieldCauldron.class, "prompt_command");
					}
				});
			}
		} else if (action.equals(AC_MUTAGEN)){
			if (Mutations.count(hero) >= Mutations.MAX_MUTATIONS){
				GLog.w(Messages.get(this, "mutagen_max"));
			} else if (Dungeon.energy < MUTAGEN_COST){
				GLog.w(Messages.get(this, "no_energy", MUTAGEN_COST));
			} else {
				GameScene.selectItem(new Distiller());
			}
		} else if (action.equals(AC_TRANSMUTE)){
			if (Dungeon.energy < transmuteCost(hero)){
				GLog.w(Messages.get(this, "no_energy", transmuteCost(hero)));
			} else {
				GameScene.selectItem(new Transmuter());
			}
		}
	}

	private void createHomunculus( Hero hero ){
		if (Homunculus.get() != null){
			GLog.w(Messages.get(this, "homunculus_exists"));
			return;
		}
		if (Dungeon.energy < HOMUNCULUS_COST){
			GLog.w(Messages.get(this, "no_energy", HOMUNCULUS_COST));
			return;
		}
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
		Dungeon.energy -= HOMUNCULUS_COST;
		Homunculus h = new Homunculus();
		h.pos = Random.element(cells);
		GameScene.add(h);
		ScrollOfTeleportation.appear(h, h.pos);
		h.sprite.emitter().burst(Speck.factory(Speck.BUBBLE), 10);
		Catalog.countUse(FieldCauldron.class);
		GLog.p(Messages.get(this, "homunculus_born"));
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
		updateQuickslot();
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

	private class Feeder extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(FieldCauldron.class, "prompt_feed");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return PotionBandolier.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Potion && (WeaponCoating.Kind.of((Potion) item) != null
					|| item instanceof PotionOfStrength || item instanceof PotionOfExperience);
		}

		@Override
		public void onSelect(Item item) {
			if (!(item instanceof Potion)) return;
			Homunculus h = Homunculus.get();
			if (h == null) return;
			String result = h.feed((Potion) item);
			if (result == null) return;
			Hero hero = Dungeon.hero;
			item.detach(hero.belongings.backpack);
			((Potion) item).identify();
			if (h.sprite != null){
				h.sprite.emitter().burst(Speck.factory(Speck.BUBBLE), 10);
				h.sprite.showStatus(CharSprite.POSITIVE, Messages.get(FieldCauldron.class, "fed"));
			}
			GLog.p(result);
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			hero.sprite.operate(hero.pos);
			hero.spendAndNext(Actor.TICK);
		}
	}

	private class Distiller extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(FieldCauldron.class, "prompt_mutagen", MUTAGEN_COST);
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return VelvetPouch.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Plant.Seed;
		}

		@Override
		public void onSelect(Item item) {
			if (!(item instanceof Plant.Seed)) return;
			Hero hero = Dungeon.hero;
			if (Dungeon.energy < MUTAGEN_COST) return;
			Dungeon.energy -= MUTAGEN_COST;
			item.detach(hero.belongings.backpack);
			Mutagen mutagen = new Mutagen();
			if (!mutagen.collect()){
				Dungeon.level.drop(mutagen, hero.pos).sprite.drop();
			}
			Catalog.countUse(FieldCauldron.class);
			GLog.p(Messages.get(FieldCauldron.class, "mutagen_done"));
			hero.sprite.emitter().burst(Speck.factory(Speck.BUBBLE), 8);
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
			hero.sprite.operate(hero.pos);
			hero.spendAndNext(Actor.TICK);
			updateQuickslot();
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
