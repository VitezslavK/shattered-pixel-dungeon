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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith.Heat;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith.Smithing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//The Smith's travelling anvil: fuse two weapons into one, reinforce a weapon with scrap,
// melt down unwanted gear into scrap, and quench a hot weapon with water from the waterskin.
public class PortableAnvil extends Item {

	public static final String AC_FUSE      = "FUSE";
	public static final String AC_REINFORCE = "REINFORCE";
	public static final String AC_SMELT     = "SMELT";
	public static final String AC_QUENCH    = "QUENCH";

	{
		image = ItemSpriteSheet.PORTABLE_ANVIL;
		defaultAction = AC_FUSE;

		unique = true;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add(AC_FUSE);
		actions.add(AC_REINFORCE);
		actions.add(AC_SMELT);
		actions.add(AC_QUENCH);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_FUSE)){
			GameScene.selectItem(new MainPicker());
		} else if (action.equals(AC_REINFORCE)){
			GameScene.selectItem(new Reinforcer());
		} else if (action.equals(AC_SMELT)){
			GameScene.selectItem(new Smelter());
		} else if (action.equals(AC_QUENCH)){
			quench(hero);
		}
	}

	private static float workTime( Hero hero ){
		return Math.max(1f, 3f - hero.pointsInTalent(Talent.QUICK_FORGE));
	}

	private void work( Hero hero, float time ){
		Catalog.countUse(PortableAnvil.class);
		Sample.INSTANCE.play(Assets.Sounds.EVOKE);
		Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH, 1, 1.3f);
		hero.sprite.centerEmitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle.FACTORY, 8);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(time);
		updateQuickslot();
	}

	private void quench( Hero hero ){
		Heat h = hero.buff(Heat.class);
		if (h == null || h.heat() < 1){
			GLog.w(Messages.get(this, "not_hot"));
			return;
		}
		Waterskin skin = hero.belongings.getItem(Waterskin.class);
		if (skin == null || skin.volume() < 1){
			GLog.w(Messages.get(this, "no_water"));
			return;
		}
		skin.useDrops(1);
		Smithing.quench(hero, h);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	private static boolean usable( Item item ){
		return item.cursedKnown && !item.cursed;
	}

	//------------------------------ fusing ------------------------------

	private class MainPicker extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(PortableAnvil.class, "prompt_main", Smithing.maxFusions(Dungeon.hero));
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof MeleeWeapon && Smithing.canBeFused((MeleeWeapon) item) && usable(item)
					&& ((MeleeWeapon) item).fusions < Smithing.maxFusions(Dungeon.hero);
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof MeleeWeapon){
				GameScene.selectItem(new DonorPicker((MeleeWeapon) item));
			}
		}
	}

	private class DonorPicker extends WndBag.ItemSelector {
		private final MeleeWeapon main;

		DonorPicker( MeleeWeapon main ){
			this.main = main;
		}

		@Override
		public String textPrompt() {
			return Messages.get(PortableAnvil.class, "prompt_donor", main.name());
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof MeleeWeapon && item != main && Smithing.canBeFused((MeleeWeapon) item)
					&& usable(item) && !item.isEquipped(Dungeon.hero);
		}

		@Override
		public void onSelect(Item item) {
			if (!(item instanceof MeleeWeapon)) return;
			Hero hero = Dungeon.hero;
			MeleeWeapon donor = (MeleeWeapon) item;
			int cost = Smithing.fusionCost(donor);
			if (!Smithing.spendScrap(hero, cost)){
				GLog.w(Messages.get(PortableAnvil.class, "no_scrap", cost));
				return;
			}
			donor.detach(hero.belongings.backpack);
			Smithing.fuse(hero, main, donor);
			GLog.p(Messages.get(PortableAnvil.class, "fused", donor.name(), main.name()));
			work(hero, workTime(hero));
		}
	}

	//------------------------------ reinforcing ------------------------------

	private class Reinforcer extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(PortableAnvil.class, "prompt_reinforce");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof MeleeWeapon && usable(item)
					&& ((MeleeWeapon) item).reinforced < Smithing.maxReinforce(Dungeon.hero);
		}

		@Override
		public void onSelect(Item item) {
			if (!(item instanceof MeleeWeapon)) return;
			Hero hero = Dungeon.hero;
			MeleeWeapon w = (MeleeWeapon) item;
			int cost = Smithing.reinforceCost(hero, w);
			if (!Smithing.spendScrap(hero, cost)){
				GLog.w(Messages.get(PortableAnvil.class, "no_scrap", cost));
				return;
			}
			w.reinforced++;
			GLog.p(Messages.get(PortableAnvil.class, "reinforced", w.name(), w.reinforced));
			work(hero, workTime(hero));
		}
	}

	//------------------------------ smelting ------------------------------

	private class Smelter extends WndBag.ItemSelector {
		@Override
		public String textPrompt() {
			return Messages.get(PortableAnvil.class, "prompt_smelt");
		}

		@Override
		public boolean itemSelectable(Item item) {
			return (item instanceof MeleeWeapon || item instanceof MissileWeapon
					|| (item instanceof Armor && !(item instanceof ClassArmor)))
					&& !item.unique && !item.isEquipped(Dungeon.hero);
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			Hero hero = Dungeon.hero;
			int value = Smithing.smeltValue(hero, item);
			item.detachAll(hero.belongings.backpack);
			Smithing.giveScrap(hero, value);
			GLog.p(Messages.get(PortableAnvil.class, "smelted", item.name(), value));
			work(hero, Actor.TICK);
		}
	}

	@Override
	public String desc() {
		Hero hero = Dungeon.hero;
		String desc = super.desc();
		if (hero != null){
			desc += "\n\n" + Messages.get(this, "desc_scrap", Smithing.scrap(hero),
					Smithing.maxFusions(hero), Smithing.maxReinforce(hero));
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

	@Override
	public int value() {
		return 0;
	}
}
