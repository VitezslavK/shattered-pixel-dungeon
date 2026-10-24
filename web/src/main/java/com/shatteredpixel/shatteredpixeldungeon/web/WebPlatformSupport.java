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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.watabou.utils.PlatformSupport;

import org.teavm.jso.JSBody;

import java.util.ArrayList;
import java.util.HashMap;

//Browser specifics: the page fills the window, fonts come from the bundled ttf files.
public class WebPlatformSupport extends PlatformSupport {

	@Override
	public void updateDisplaySize() {
		//the canvas always fills the browser window
	}

	//browsers report a density of 1, the device pixel ratio matches what the game gets natively
	@JSBody(script = "return window.devicePixelRatio || 1;")
	private static native double devicePixelRatio();

	@Override
	public float densityOverride() {
		return (float) devicePixelRatio();
	}

	@Override
	public void updateSystemUI() {
		//no system UI to change in a browser
	}

	@Override
	public boolean connectedToUnmeteredNetwork() {
		return true;
	}

	@Override
	public boolean supportsVibration() {
		return false;
	}

	private static FreeTypeFontGenerator basicFontGenerator;
	private static FreeTypeFontGenerator asianFontGenerator;

	@Override
	public void setupFontGenerators(int pageSize, boolean systemfont) {
		if (fonts != null && this.pageSize == pageSize && this.systemfont == systemfont){
			return;
		}
		this.pageSize = pageSize;
		this.systemfont = systemfont;

		resetGenerators(false);
		fonts = new HashMap<>();

		//only the pixel font is shipped to browsers (the 3.6MB droid sans fallback is left out),
		// so asian languages and the 'system font' setting fall back to it as well
		basicFontGenerator = asianFontGenerator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/pixel_font.ttf"));

		fonts.put(basicFontGenerator, new HashMap<>());

		packer = new PixmapPacker(pageSize, pageSize, Pixmap.Format.RGBA8888, 1, false);
	}

	//hangul, CJK ideographs and symbols, hiragana, katakana, half/full width forms
	private static boolean isAsian(char c){
		return (c >= 0xAC00 && c <= 0xD7AF) || (c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F)
				|| (c >= 0xFF00 && c <= 0xFFEF) || (c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF);
	}

	@Override
	protected FreeTypeFontGenerator getGeneratorForString( String input ){
		for (int i = 0; i < input.length(); i++){
			if (isAsian(input.charAt(i))) return asianFontGenerator;
		}
		return basicFontGenerator;
	}

	//same splitting as the desktop version, written without regex lookbehind:
	// breaks around newlines, '_' and '**' (highlighting), asian characters, and spaces when multiline
	@Override
	public String[] splitforTextBlock(String text, boolean multiline) {
		ArrayList<String> parts = new ArrayList<>();
		StringBuilder cur = new StringBuilder();
		int i = 0;
		while (i < text.length()){
			char c = text.charAt(i);
			String token = null;
			if (c == '\n' || c == '_' || isAsian(c) || (multiline && c == ' ')){
				token = String.valueOf(c);
			} else if (c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*'){
				token = "**";
			}
			if (token != null){
				if (cur.length() > 0){
					parts.add(cur.toString());
					cur.setLength(0);
				}
				parts.add(token);
				i += token.length();
			} else {
				cur.append(c);
				i++;
			}
		}
		if (cur.length() > 0) parts.add(cur.toString());
		return parts.toArray(new String[0]);
	}
}
