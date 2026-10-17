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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.arcanist.Arcana;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.EnergyParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

//The arcanist's class item. Breaks magical items down into arcane essence, which is spent to
// recharge wands and artifacts, permanently empower wands and rings, or (runesmith) inscribe runes.
public class ArcaneCodex extends Item {

	public static final String AC_DISENCHANT = "DISENCHANT";
	public static final String AC_INFUSE     = "INFUSE";
	public static final String AC_EMPOWER    = "EMPOWER";
	public static final String AC_INSCRIBE   = "INSCRIBE";

	{
		image = ItemSpriteSheet.ARCANE_CODEX;
		defaultAction = AC_DISENCHANT;

		unique = true;
		bones = false;
	}

	public int essence = 0;

	public static final int INFUSE_COST = 2;
	public static final int BASE_INSCRIBE_COST = 15;
	//the codex can't push items past this level, beyond that they need scrolls of upgrade
	public static final int MAX_EMPOWER_LEVEL = 5;

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add(AC_DISENCHANT);
		actions.add(AC_INFUSE);
		actions.add(AC_EMPOWER);
		if (hero.subClass == HeroSubClass.RUNESMITH){
			actions.add(AC_INSCRIBE);
		}
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_DISENCHANT)){
			GameScene.selectItem(new Disenchanter());
		} else if (action.equals(AC_INFUSE)){
			if (essence < INFUSE_COST){
				GLog.w(Messages.get(this, "no_essence"));
			} else {
				GameScene.selectItem(new Infuser());
			}
		} else if (action.equals(AC_EMPOWER)){
			GameScene.selectItem(new Empowerer());
		} else if (action.equals(AC_INSCRIBE)){
			if (essence < inscribeCost(hero)){
				GLog.w(Messages.get(this, "no_essence"));
			} else {
				GameScene.selectItem(new Inscriber());
			}
		}
	}

	public void gainEssence( int amount ){
		essence += amount;
		updateQuickslot();
	}

	public static void giveEssence( Hero hero, int amount ){
		ArcaneCodex codex = hero.belongings.getItem(ArcaneCodex.class);
		if (codex != null && amount > 0){
			codex.gainEssence(amount);
		}
	}

	//how much essence an item breaks down into
	public static int essenceValue( Item item ){
		int lvl = item.levelKnown ? Math.max(0, item.level()) : 0;
		int value;
		if (item instanceof Artifact){
			value = 6 + 2*lvl;
		} else if (item instanceof Wand || item instanceof Ring){
			value = 4 + 3*lvl;
		} else if (item instanceof Weapon || item instanceof Armor){
			value = 2 + 2*lvl;
			if (item instanceof Weapon && ((Weapon) item).enchantment != null && !((Weapon) item).hasCurseEnchant()) value += 3;
			if (item instanceof Armor && ((Armor) item).glyph != null && !((Armor) item).hasCurseGlyph()) value += 3;
		} else {
			value = 1;
		}
		return value * item.quantity();
	}

	public static boolean canDisenchant( Item item ){
		if (item.unique || item instanceof ArcaneCodex || item instanceof MagesStaff) return false;
		if (item.isEquipped(Dungeon.hero)) return false;
		return item instanceof Artifact || item instanceof Wand || item instanceof Ring
				|| item instanceof Weapon || item instanceof Armor
				|| item instanceof Scroll || item instanceof Potion || item instanceof Runestone;
	}

	public static int empowerCost( Hero hero, Item item ){
		float cost = 15 + 9*Math.max(0, item.level());
		cost *= 1f - 0.1f*hero.pointsInTalent(Talent.ARCANE_EMPOWERMENT);
		return Math.max(1, Math.round(cost));
	}

	public static int inscribeCost( Hero hero ){
		return BASE_INSCRIBE_COST - 3*hero.pointsInTalent(Talent.RUNIC_DISCOUNT);
	}

	private void spend( Hero hero, int cost ){
		essence -= cost;
		updateQuickslot();
		Arcana.onCodexUsed(hero);
		hero.sprite.operate(hero.pos);
		hero.sprite.centerEmitter().burst(EnergyParticle.FACTORY, 8);
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
		hero.spendAndNext(Actor.TICK);
	}

	@Override
	public String status() {
		return Integer.toString(essence);
	}

	@Override
	public String desc() {
		String desc = super.desc();
		desc += "\n\n" + Messages.get(this, "desc_essence", essence);
		if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.RUNESMITH){
			desc += "\n\n" + Messages.get(this, "desc_inscribe", inscribeCost(Dungeon.hero));
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

	private static final String ESSENCE = "essence";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(ESSENCE, essence);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		essence = bundle.getInt(ESSENCE);
	}

	private class Disenchanter extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(ArcaneCodex.class, "prompt_disenchant");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return canDisenchant(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null || !canDisenchant(item)) return;
			Hero hero = Dungeon.hero;
			int gained = Math.round(essenceValue(item) * Arcana.disenchantFactor(hero));
			item.detachAll(hero.belongings.backpack);
			Catalog.countUse(ArcaneCodex.class);
			essence += gained;
			GLog.p(Messages.get(ArcaneCodex.class, "disenchanted", item.name(), gained));
			updateQuickslot();
			Arcana.onCodexUsed(hero);
			hero.sprite.operate(hero.pos);
			hero.sprite.centerEmitter().burst(Speck.factory(Speck.STAR), 6);
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
			hero.spendAndNext(Actor.TICK);
		}
	}

	private class Infuser extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(ArcaneCodex.class, "prompt_infuse", INFUSE_COST);
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Wand || item instanceof Artifact || item instanceof MagesStaff;
		}

		@Override
		public void onSelect(Item item) {
			if (item == null || essence < INFUSE_COST) return;
			Hero hero = Dungeon.hero;
			float boost = 1f + 0.5f*hero.pointsInTalent(Talent.EFFICIENT_INFUSION);
			if (item instanceof Wand){
				((Wand) item).gainCharge(boost, true);
			} else if (item instanceof MagesStaff){
				((MagesStaff) item).gainCharge(boost, true);
			} else if (item instanceof Artifact){
				((Artifact) item).charge(hero, 15f * boost);
			}
			GLog.p(Messages.get(ArcaneCodex.class, "infused", item.name()));
			spend(hero, INFUSE_COST);
		}
	}

	private class Empowerer extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(ArcaneCodex.class, "prompt_empower");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return (item instanceof Wand || item instanceof Ring)
					&& item.isIdentified() && !item.cursed && item.level() < MAX_EMPOWER_LEVEL;
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			Hero hero = Dungeon.hero;
			int cost = empowerCost(hero, item);
			if (essence < cost){
				GLog.w(Messages.get(ArcaneCodex.class, "no_essence_cost", cost));
				return;
			}
			item.upgrade();
			Catalog.countUse(ArcaneCodex.class);
			GLog.p(Messages.get(ArcaneCodex.class, "empowered", item.name()));
			spend(hero, cost);
		}
	}

	private class Inscriber extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(ArcaneCodex.class, "prompt_inscribe");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return (item instanceof Weapon && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon))
					|| item instanceof Armor;
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			Hero hero = Dungeon.hero;
			int cost = inscribeCost(hero);
			if (essence < cost) return;
			boolean rare = Arcana.inscribeRare(hero);
			if (item instanceof Weapon){
				Weapon w = (Weapon) item;
				w.enchant(rare ? Weapon.Enchantment.randomRare() : Weapon.Enchantment.random());
			} else {
				Armor a = (Armor) item;
				a.inscribe(rare ? Armor.Glyph.randomRare() : Armor.Glyph.random());
			}
			item.cursed = false;
			GLog.p(Messages.get(ArcaneCodex.class, "inscribed", item.name()));
			spend(hero, cost);
		}
	}
}
