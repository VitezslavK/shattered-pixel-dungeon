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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.watabou.utils.Callback;
import com.watabou.noosa.Game;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ModMobFrames;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//An ancient shrine that offers the hero a deal. Each shrine can be used once.
public class Shrine extends NPC {

	public static final int BLOOD = 0;  //pay health for an upgrade
	public static final int GOLD  = 1;  //pay gold for healing and a blessing
	public static final int CHANCE= 2;  //offer an item to gamble for a better one

	{
		spriteClass = ShrineSprite.class;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	public int type = BLOOD;

	public static Shrine random(){
		Shrine s = new Shrine();
		s.type = Random.Int(3);
		return s;
	}

	@Override
	protected boolean act() {
		if (Dungeon.level.heroFOV[pos]) {
			com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary.setSeen(getClass());
		}
		throwItems();
		spend(TICK);
		return true;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage(int dmg, Object src) {
		//shrines can't be harmed
	}

	@Override
	public boolean add(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff buff) {
		return false;
	}

	@Override
	public String name() {
		return Messages.get(this, "name_" + type);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc_" + type, goldCost());
	}

	private int goldCost(){
		return 40 + 15*Dungeon.depth;
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero) return true;
		final Hero hero = (Hero) c;
		//interactions happen on the actor thread, windows must be created on the render thread
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show(new WndOptions(sprite(), Messages.titleCase(name()),
						Messages.get(Shrine.this, "offer_" + type, goldCost()),
						Messages.get(Shrine.this, "accept"), Messages.get(Shrine.this, "refuse")){
					@Override
					protected void onSelect(int index) {
						if (index == 0) accept(hero);
					}
				});
			}
		});
		return true;
	}

	private void accept( Hero hero ){
		switch (type){
			case BLOOD: default:
				Item target = Random.Int(2) == 0 ? hero.belongings.weapon() : hero.belongings.armor();
				if (target == null) target = hero.belongings.armor() != null ? hero.belongings.armor() : hero.belongings.weapon();
				if (target == null || !target.isUpgradable()){
					GLog.w(Messages.get(this, "nothing"));
					return;
				}
				hero.damage(Math.max(1, hero.HP/4), this);
				target.upgrade();
				Item.updateQuickslot();
				GLog.p(Messages.get(this, "blood_done", target.name()));
				break;
			case GOLD:
				if (Dungeon.gold < goldCost()){
					GLog.w(Messages.get(this, "no_gold"));
					return;
				}
				Dungeon.gold -= goldCost();
				hero.HP = hero.HT;
				Buff.prolong(hero, Bless.class, 50f);
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 6);
				GLog.p(Messages.get(this, "gold_done"));
				break;
			case CHANCE:
				GameScene.selectItem(new WndBag.ItemSelector() {
					@Override
					public String textPrompt() {
						return Messages.get(Shrine.class, "chance_prompt");
					}

					@Override
					public boolean itemSelectable(Item item) {
						return !item.unique && !item.isEquipped(hero)
								&& (item instanceof Weapon || item instanceof Armor
								|| Generator.Category.ARTIFACT.superClass.isInstance(item)
								|| Generator.Category.RING.superClass.isInstance(item)
								|| Generator.Category.WAND.superClass.isInstance(item));
					}

					@Override
					public void onSelect(Item item) {
						if (item == null) return;
						item.detachAll(hero.belongings.backpack);
						if (Random.Int(2) == 0){
							Item reward = Generator.randomUsingDefaults(Generator.Category.WAND.superClass.isInstance(item) ? Generator.Category.WAND
									: Generator.Category.RING.superClass.isInstance(item) ? Generator.Category.RING
									: item instanceof Armor ? Generator.Category.ARMOR : Generator.Category.WEAPON);
							if (reward != null){
								if (reward.isUpgradable()) reward.upgrade();
								reward.cursed = false;
								reward.identify();
								if (!reward.collect()) Dungeon.level.drop(reward, hero.pos).sprite.drop();
								GLog.p(Messages.get(Shrine.class, "chance_win", reward.name()));
							}
						} else {
							GLog.n(Messages.get(Shrine.class, "chance_lose", item.name()));
						}
						finishUse();
					}
				});
				return;
		}
		finishUse();
	}

	private void finishUse(){
		Sample.INSTANCE.play(Assets.Sounds.MASTERY);
		if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.LIGHT), 10);
		destroy();
		if (sprite != null) sprite.killAndErase();
		Dungeon.level.mobs.remove(this);
	}

	private static final String TYPE = "type";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TYPE, type);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		type = bundle.getInt(TYPE);
	}

	public static class ShrineSprite extends StatueSprite {
		@Override
		public void link(Char ch) {
			super.link(ch);
			String sheet = "sprites/mod/shrine_arcane.png";
			if (ch instanceof Shrine){
				switch (((Shrine) ch).type){
					case BLOOD: sheet = "sprites/mod/shrine_blood.png"; break;
					case GOLD:  sheet = "sprites/mod/shrine_gold.png"; break;
					default: break;
				}
			}
			ModMobFrames.apply( this, sheet, 14, 16, 3, 3 );
		}
	}
}
