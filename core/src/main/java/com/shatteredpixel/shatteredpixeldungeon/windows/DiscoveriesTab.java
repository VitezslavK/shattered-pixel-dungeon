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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.journal.Discoveries;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Reactions;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Runes;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

//journal tab listing the elemental reactions the player has discovered across all runs
public class DiscoveriesTab extends Component {

	private RenderedTextBlock title;
	private ScrollPane list;
	private final ArrayList<RenderedTextBlock> entries = new ArrayList<>();
	private final ArrayList<ColorBlock> separators = new ArrayList<>();

	@Override
	protected void createChildren() {
		title = PixelScene.renderTextBlock(9);
		title.hardlight(Window.TITLE_COLOR);
		add(title);

		list = new ScrollPane(new Component());
		add(list);
	}

	public void updateList(){
		title.text(Messages.get(this, "title",
				Discoveries.count() + Discoveries.runeCount(),
				Reactions.Reaction.values().length + Runes.Rune.values().length));

		Component content = list.content();
		content.clear();
		entries.clear();
		separators.clear();

		addHeader(Messages.get(this, "reactions"));
		for (Reactions.Reaction r : Reactions.Reaction.values()){
			RenderedTextBlock entry;
			if (Discoveries.isDiscovered(r)){
				entry = PixelScene.renderTextBlock("_" + Messages.titleCase(r.title()) + "_\n" + r.desc(), 6);
			} else {
				entry = PixelScene.renderTextBlock("_???_\n" + r.hint(), 6);
				entry.alpha(0.6f);
			}
			content.add(entry);
			entries.add(entry);

			ColorBlock sep = new ColorBlock(1, 1, 0xFF222222);
			content.add(sep);
			separators.add(sep);
		}

		addHeader(Messages.get(this, "runes"));
		for (Runes.Rune r : Runes.Rune.values()){
			RenderedTextBlock entry;
			if (r.active()){
				entry = PixelScene.renderTextBlock("_" + Messages.titleCase(r.title()) + "_\n" + r.desc(), 6);
			} else {
				entry = PixelScene.renderTextBlock("_???_\n" + Messages.get(this, "rune_hint"), 6);
				entry.alpha(0.6f);
			}
			content.add(entry);
			entries.add(entry);

			ColorBlock sep = new ColorBlock(1, 1, 0xFF222222);
			content.add(sep);
			separators.add(sep);
		}
		layout();
	}

	private void addHeader( String text ){
		RenderedTextBlock header = PixelScene.renderTextBlock(text, 8);
		header.hardlight(Window.TITLE_COLOR);
		list.content().add(header);
		entries.add(header);
		ColorBlock sep = new ColorBlock(1, 1, 0xFF222222);
		list.content().add(sep);
		separators.add(sep);
	}

	@Override
	protected void layout() {
		super.layout();
		title.setPos(x + (width - title.width())/2f, y + 2);
		PixelScene.align(title);

		float top = 2;
		for (int i = 0; i < entries.size(); i++){
			RenderedTextBlock e = entries.get(i);
			e.maxWidth((int)width - 4);
			e.setPos(2, top);
			top = e.bottom() + 3;
			ColorBlock sep = separators.get(i);
			sep.size(width, 1);
			sep.x = 0;
			sep.y = top;
			top += 4;
		}
		list.content().setSize(width, top);
		list.setRect(x, title.bottom() + 4, width, height - (title.bottom() + 4 - y));
		list.scrollTo(0, 0);
	}
}
