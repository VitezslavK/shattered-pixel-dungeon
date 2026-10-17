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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

//dropped by Yog-Dzewa, lets the hero evolve their subclass
public class EssenceOfAscension extends Item {

	private static final String AC_ABSORB = "ABSORB";

	{
		stackable = false;
		image = ItemSpriteSheet.ESSENCE;

		defaultAction = AC_ABSORB;

		unique = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_ABSORB );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_ABSORB )) {

			if (hero.subClass == HeroSubClass.NONE){
				GLog.w(Messages.get(this, "no_subclass"));
				return;
			}
			if (hero.evolved){
				GLog.w(Messages.get(this, "already_evolved"));
				return;
			}

			HeroSubClass sub = hero.subClass;
			GameScene.show(new WndOptions(
					new HeroIcon(sub.evolutionIcon()),
					Messages.titleCase(sub.evolutionTitle()),
					sub.evolutionDesc() + "\n\n" + Messages.get(this, "confirm"),
					Messages.get(this, "yes"),
					Messages.get(this, "no")){
				@Override
				protected void onSelect(int index) {
					if (index == 0){
						absorb(hero);
					}
				}
			});
		}
	}

	private void absorb( Hero hero ){
		detach( hero.belongings.backpack );
		Catalog.countUse( getClass() );

		Evolution.onEvolve(hero);

		hero.sprite.operate( hero.pos );
		hero.sprite.emitter().burst( ShadowParticle.UP, 12 );
		hero.sprite.centerEmitter().start( Speck.factory( Speck.UP ), 0.1f, 10 );
		Sample.INSTANCE.play( Assets.Sounds.MASTERY );

		GLog.p( Messages.get(this, "evolved", Messages.titleCase(hero.subClass.evolutionTitle())) );

		hero.spend( Actor.TICK );
		hero.busy();
		updateQuickslot();
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero != null
				&& com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.subClass != HeroSubClass.NONE){
			HeroSubClass sub = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero.subClass;
			desc += "\n\n" + Messages.get(this, "your_evolution", Messages.titleCase(sub.evolutionTitle()));
		}
		return desc;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing(0x66FFAA, 1f);
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}
}
