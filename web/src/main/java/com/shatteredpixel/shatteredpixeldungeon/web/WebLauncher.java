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

package com.shatteredpixel.shatteredpixeldungeon.web;

import com.badlogic.gdx.Files;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.watabou.noosa.Game;
import com.watabou.utils.FileUtils;
import com.watabou.utils.Reflection;

//Starts the game in a browser page; saves and settings live in the browser's storage.
public class WebLauncher {

	public static void main(String[] args) {
		Game.version = "3.3.8";
		Game.versionCode = 896;
		FileUtils.setDefaultFileProperties(Files.FileType.Local, "");
		//saves are restored by class name, which TeaVM can't do by itself
		Reflection.factory = new WebFactory();

		WebApplicationConfiguration config = new WebApplicationConfiguration();
		config.width = 0;
		config.height = 0;
		config.showDownloadLogs = false;
		//render at the screen's real resolution so the pixel art stays sharp on phones
		config.usePhysicalPixels = true;
		//the FreeType font engine is a separate script that has to load before the game starts
		config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");
		new WebApplication(new ShatteredPixelDungeon(new WebPlatformSupport()), config);
	}
}
