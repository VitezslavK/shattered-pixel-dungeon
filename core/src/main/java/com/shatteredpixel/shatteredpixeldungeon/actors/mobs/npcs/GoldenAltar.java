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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.gold.GoldItems;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ModMobFrames;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.CurrencyIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

import java.util.ArrayList;

//Stands near the entrance of the first floor of each region. It offers three golden items
// (a piece of gear, a consumable and a talisman); the hero pays gold for one, then it crumbles.
public class GoldenAltar extends NPC {

	{
		spriteClass = AltarSprite.class;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
	}

	private ArrayList<Item> offers = new ArrayList<>();

	//the sewers' altar is on floor 2, the hero has no gold yet on floor 1
	public static boolean appearsOn( int depth ){
		return depth == 2 || (depth > 5 && depth % 5 == 1);
	}

	public static GoldenAltar create(){
		GoldenAltar a = new GoldenAltar();
		a.offers.add(GoldItems.random(GoldItems.Kind.EQUIPMENT, Dungeon.depth));
		a.offers.add(GoldItems.random(GoldItems.Kind.CONSUMABLE, Dungeon.depth));
		a.offers.add(GoldItems.random(GoldItems.Kind.TALISMAN, Dungeon.depth));
		return a;
	}

	//prices grow with each region, the gear and talisman cost more than the consumable
	public static int price( int index ){
		int region = 1 + Dungeon.depth / 5;
		switch (index){
			case 0: default: return 60 * region;
			case 1: return 35 * region;
			case 2: return 90 * region;
		}
	}

	@Override
	protected boolean act() {
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
		//altars can't be harmed
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	@Override
	public boolean interact(Char c) {
		if (c != Dungeon.hero || offers.size() < 3) return true;
		//interactions happen on the actor thread, windows must be created on the render thread
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				String[] options = new String[4];
				for (int i = 0; i < 3; i++){
					options[i] = Messages.get(GoldenAltar.class, "offer", price(i), Messages.titleCase(offers.get(i).title()));
				}
				options[3] = Messages.get(GoldenAltar.class, "leave");
				CurrencyIndicator.showGold = true;
				GameScene.show(new WndOptions(sprite(), Messages.titleCase(name()), description(), options){
					@Override
					protected void onSelect(int index) {
						if (index >= 0 && index < 3) buy(index);
					}

					@Override
					protected boolean enabled(int index) {
						return index == 3 || Dungeon.gold >= price(index);
					}

					@Override
					protected boolean hasIcon(int index) {
						return index < 3;
					}

					@Override
					protected Image getIcon(int index) {
						return index < 3 ? new ItemSprite(offers.get(index)) : null;
					}

					@Override
					protected boolean hasInfo(int index) {
						return index < 3;
					}

					@Override
					protected void onInfo(int index) {
						GameScene.show(new WndInfoItem(offers.get(index)));
					}

					@Override
					public void hide() {
						super.hide();
						CurrencyIndicator.showGold = false;
					}
				});
			}
		});
		return true;
	}

	private void buy( int index ){
		int cost = price(index);
		if (Dungeon.gold < cost) return;
		Dungeon.gold -= cost;
		Item item = offers.get(index);
		offers.clear();
		if (!item.doPickUp(Dungeon.hero)){
			Dungeon.level.drop(item, Dungeon.hero.pos);
		}
		GLog.p(Messages.get(this, "bought", Messages.titleCase(item.title())));
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		CellEmitter.get(pos).burst(Speck.factory(Speck.COIN), 10);
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	private static final String OFFERS = "offers";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(OFFERS, offers);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		offers.clear();
		for (Bundlable b : bundle.getCollection(OFFERS)){
			offers.add((Item) b);
		}
	}

	public static class AltarSprite extends StatueSprite {
		public AltarSprite(){
			super();
			ModMobFrames.apply( this, "sprites/mod/golden_altar.png", 16, 18, 3, 3 );
		}
	}
}
