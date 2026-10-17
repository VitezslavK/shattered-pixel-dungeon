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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.watabou.noosa.TextureFilm;

//a translucent copy of another player's hero, used by invading ghosts and summoned ghost allies
public class GhostHeroSprite extends MobSprite {

	public interface GhostHero {
		HeroClass ghostClass();
		int ghostArmorTier();
		boolean ghostHostile();
	}

	private boolean hostile = true;

	public GhostHeroSprite() {
		super();
		setup(HeroClass.WARRIOR, 0);
	}

	public void setup( HeroClass cls, int tier ){
		texture(cls.spritesheet());

		TextureFilm film = new TextureFilm( HeroSprite.tiers(), Math.max(0, Math.min(6, tier)), 12, 15 );

		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );

		run = new Animation( 20, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );

		die = new Animation( 20, false );
		die.frames( film, 0 );

		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );

		play(idle, true);
		resetColor();
	}

	@Override
	public void link( Char ch ) {
		super.link(ch);
		if (ch instanceof GhostHero){
			hostile = ((GhostHero) ch).ghostHostile();
			setup(((GhostHero) ch).ghostClass(), ((GhostHero) ch).ghostArmorTier());
		}
	}

	@Override
	public void resetColor() {
		super.resetColor();
		alpha(0.75f);
		if (hostile) tint(0.8f, 0.2f, 0.9f, 0.5f);
		else         tint(0.3f, 0.8f, 1f, 0.5f);
	}

	@Override
	public void die() {
		super.die();
		emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
	}

	@Override
	public void draw() {
		if (alpha() >= 0.75f) alpha(0.75f);
		super.draw();
	}
}
