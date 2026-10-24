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

import com.github.xpenatan.gdx.teavm.backends.shared.config.AssetFileHandle;
import com.github.xpenatan.gdx.teavm.backends.shared.config.compiler.TeaCompiler;
import com.github.xpenatan.gdx.teavm.backends.web.config.backend.WebBackend;

import org.teavm.vm.TeaVMOptimizationLevel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

//Compiles the game to JavaScript. Run through: ./gradlew web:buildWeb
//args: assets folder, output folder, "true" to start a local server afterwards, (unused), folder with our page files
public class BuildWeb {

	public static void main(String[] args) {
		boolean serve = args.length > 2 && args[2].equals("true");
		new TeaCompiler(new WebBackend().setStartJettyAfterBuild(serve))
				.addAssets(new AssetFileHandle(args[0]))
				//ADVANCED and obfuscation both break this TeaVM version (missing functions at startup), SIMPLE works;
				// the file is large but compresses well (~3.5MB gzipped, which is what GitHub Pages sends)
				.setOptimizationLevel(TeaVMOptimizationLevel.SIMPLE)
				.setMainClass(WebLauncher.class.getName())
				.setObfuscated(false)
				//everything the game saves is restored by class name
				.addReflectionClass("com.shatteredpixel.shatteredpixeldungeon")
				.addReflectionClass("com.watabou")
				.build(new File(args[1]));

		//our own page (home screen app settings, title, loading text), then drop the server-only files
		try {
			File webapp = new File(args[1], "webapp");
			File own = new File(args[4]);
			for (File f : own.listFiles()){
				Files.copy(f.toPath(), new File(webapp, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
			deleteAll(new File(webapp, "WEB-INF"));
			deleteAll(new File(webapp, "assets/webapp"));
			new File(webapp, "assets/online.properties.example").delete();
			//the game's icon instead of the libGDX logo on the loading screen (the asset list records sizes)
			File logo = new File(webapp, "assets/startup-logo.png");
			Files.copy(new File(own, "icon-256.png").toPath(), logo.toPath(), StandardCopyOption.REPLACE_EXISTING);
			File list = new File(webapp, "assets/preload.txt");
			if (list.exists()){
				String txt = Files.readString(list.toPath());
				txt = txt.replaceAll("(:/startup-logo\\.png:)\\d+", "$1" + logo.length());
				Files.writeString(list.toPath(), txt);
			}
			//GitHub Pages: serve files as they are
			new File(webapp, ".nojekyll").createNewFile();
		} catch (IOException e){
			throw new RuntimeException(e);
		}
	}

	private static void deleteAll(File f){
		File[] children = f.listFiles();
		if (children != null) for (File c : children) deleteAll(c);
		f.delete();
	}
}
