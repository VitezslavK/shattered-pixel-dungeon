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

import com.watabou.noosa.MovieClip;
import com.watabou.noosa.TextureFilm;

//The mod's own monster sheets (drawn by tools/art) share one 18 frame layout:
// 0-3 idle, 4-7 run, 8-10 attack, 11-14 die, 15-17 zap.
//Sprites keep extending their old base class so its effects (bolts, particles, beams) still work,
// they only swap in the new texture and animations after the base constructor ran.
public class ModMobFrames {

	public static TextureFilm apply( CharSprite s, String texture, int w, int h, int idleFps, int runFps ){
		s.texture( texture );
		TextureFilm f = new TextureFilm( s.texture, w, h );

		s.idle = new MovieClip.Animation( idleFps, true );
		s.idle.frames( f, 0, 1, 2, 3 );

		s.run = new MovieClip.Animation( runFps, true );
		s.run.frames( f, 4, 5, 6, 7 );

		s.attack = new MovieClip.Animation( 12, false );
		s.attack.frames( f, 8, 9, 10, 0 );

		s.zap = new MovieClip.Animation( 10, false );
		s.zap.frames( f, 15, 16, 17, 0 );

		s.die = new MovieClip.Animation( 10, false );
		s.die.frames( f, 11, 12, 13, 14 );

		s.play( s.idle );
		return f;
	}

	public static TextureFilm apply( CharSprite s, String texture, int w, int h ){
		return apply( s, texture, w, h, 3, 12 );
	}
}
