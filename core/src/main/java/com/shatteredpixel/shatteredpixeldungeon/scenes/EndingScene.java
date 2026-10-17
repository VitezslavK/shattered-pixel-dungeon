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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Endings;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.RectF;

//epilogue screen shown at the end of a run, the text depends on which ending was reached
public class EndingScene extends PixelScene {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 20;
	private static final float GAP      = 8;

	public static Endings.Ending ending = Endings.Ending.ESCAPE;

	@Override
	public void create() {
		super.create();

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_2, Assets.Music.THEME_1},
				new float[]{1, 1},
				false);

		RectF insets = getCommonInsets();
		int w = (int) (Camera.main.width - insets.left - insets.right);
		int h = (int) (Camera.main.height - insets.top - insets.bottom);
		int textWidth = landscape() ? 2*WIDTH : WIDTH;

		Image amulet = new Image( Assets.Sprites.AMULET );
		if (ending == Endings.Ending.DARK_LORD){
			amulet.hardlight(0.6f, 0.1f, 0.6f);
		} else if (ending == Endings.Ending.DESTROYER){
			amulet.alpha(0.4f);
		} else if (ending == Endings.Ending.KEEPER){
			amulet.hardlight(0.4f, 0.9f, 1f);
		}
		add(amulet);

		RenderedTextBlock title = renderTextBlock( Messages.titleCase(ending.title()), 9 );
		title.hardlight(Window.TITLE_COLOR);
		add(title);

		RenderedTextBlock text = renderTextBlock( ending.text(), 6 );
		text.maxWidth(textWidth);
		add(text);

		StyledButton btnContinue = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "continue")) {
			@Override
			protected void onClick() {
				Game.switchScene( RankingsScene.class );
			}
		};
		btnContinue.setSize(WIDTH, BTN_HEIGHT);
		add(btnContinue);

		float height = amulet.height + GAP + title.height() + GAP + text.height() + GAP + BTN_HEIGHT;
		float top = insets.top + Math.max(0, (h - height) / 2);

		amulet.x = insets.left + (w - amulet.width) / 2;
		amulet.y = top;
		align(amulet);

		title.setPos(insets.left + (w - title.width()) / 2, amulet.y + amulet.height + GAP);
		align(title);

		text.setPos(insets.left + (w - text.width()) / 2, title.bottom() + GAP);
		align(text);

		btnContinue.setPos(insets.left + (w - WIDTH) / 2, text.bottom() + GAP);

		new Flare( 8, 48 ).color( ending == Endings.Ending.DARK_LORD ? 0xAA22AA : 0xFFDDBB, true ).show( amulet, 0 ).angularSpeed = +30;

		fadeIn();
	}

	@Override
	protected void onBackPressed() {
		Game.switchScene( RankingsScene.class );
	}
}
