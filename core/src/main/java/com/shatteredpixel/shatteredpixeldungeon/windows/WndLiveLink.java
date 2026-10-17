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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.online.LiveLink;
import com.shatteredpixel.shatteredpixeldungeon.online.OnlineService;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;

import java.util.ArrayList;

//Rooms for the live link: create or join one, see friends' runs live, send them gifts.
public class WndLiveLink extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP        = 2;

	private float pos;

	//shows the window right away, then again with fresh data once the room has been read
	public static void open(){
		final WndLiveLink w = new WndLiveLink();
		Game.scene().addToFront(w);
		LiveLink.poll(new Runnable() {
			@Override
			public void run() {
				if (w.parent != null){
					w.hide();
					Game.scene().addToFront(new WndLiveLink());
				}
			}
		});
	}

	public WndLiveLink(){
		super();

		IconTitle title = new IconTitle(Icons.get(Icons.TALENT), Messages.get(this, "title"));
		title.setRect(0, 0, WIDTH, 0);
		add(title);
		pos = title.bottom() + 4;

		if (!OnlineService.enabled()){
			addText(Messages.get(this, "offline"));
			resize(WIDTH, (int)pos);
			return;
		}

		if (!LiveLink.inRoom()){
			addText(Messages.get(this, "intro"));
			addButton(new RedButton(Messages.get(this, "create")) {
				@Override
				protected void onClick() {
					LiveLink.createRoom();
					reopen();
				}
			});
			addButton(new RedButton(Messages.get(this, "join")) {
				@Override
				protected void onClick() {
					Game.scene().addToFront(new WndTextInput(Messages.get(WndLiveLink.class, "join_title"),
							Messages.get(WndLiveLink.class, "join_body"), "", 11, false,
							Messages.get(WndLiveLink.class, "confirm"), Messages.get(WndLiveLink.class, "cancel")){
						@Override
						public void onSelect(boolean positive, String text) {
							if (!positive) return;
							if (!LiveLink.joinRoom(text)){
								GLog.w(Messages.get(WndLiveLink.class, "bad_code"));
							}
							WndLiveLink.this.hide();
							open();
						}
					});
				}
			});
			resize(WIDTH, (int)pos - GAP);
			return;
		}

		String room = LiveLink.room();
		addText(Messages.get(this, "room", room) + "\n\n"
				+ Messages.get(this, LiveLink.activeRun() ? "playing" : "start_hint"));

		ArrayList<LiveLink.Friend> friends = LiveLink.friends();
		StringBuilder list = new StringBuilder();
		if (friends.isEmpty()){
			list.append(Messages.get(this, "nobody"));
		}
		for (LiveLink.Friend f : friends){
			if (list.length() > 0) list.append("\n");
			list.append("_").append(f.name).append("_ (").append(Messages.titleCase(f.heroClass.title())).append(") ");
			if (f.won)       list.append(Messages.get(this, "f_won"));
			else if (f.dead) list.append(Messages.get(this, "f_dead", f.depth));
			else             list.append(Messages.get(this, "f_status", f.depth, f.lvl, f.hp, f.ht));
		}
		addText(list.toString());

		if (LiveLink.activeRun()){
			if (LiveLink.canGift()){
				for (final LiveLink.Friend f : friends){
					if (f.dead || f.won) continue;
					addButton(new RedButton(Messages.get(this, "gift", f.name)) {
						@Override
						protected void onClick() {
							Game.scene().addToFront(new WndOptions(Icons.get(Icons.TALENT),
									Messages.get(WndLiveLink.class, "gift_title", f.name),
									Messages.get(WndLiveLink.class, "gift_body"),
									Messages.get(WndLiveLink.class, "gift_help"),
									Messages.get(WndLiveLink.class, "gift_mischief")){
								@Override
								protected void onSelect(int index) {
									if (index < 0) return;
									if (LiveLink.sendGift(f, index == 0)){
										GLog.p(Messages.get(WndLiveLink.class, index == 0 ? "sent_help" : "sent_mischief", f.name));
									}
									WndLiveLink.this.hide();
								}
							});
						}
					});
				}
			} else {
				addText(Messages.get(this, "gift_used"));
			}
		}

		addButton(new RedButton(Messages.get(this, "refresh")) {
			@Override
			protected void onClick() {
				hide();
				open();
			}
		});
		RedButton leave = new RedButton(Messages.get(this, "leave")) {
			@Override
			protected void onClick() {
				LiveLink.leave();
				reopen();
			}
		};
		leave.textColor(0xFF8888);
		addButton(leave);

		resize(WIDTH, (int)pos - GAP);
	}

	private void addText( String text ){
		RenderedTextBlock block = PixelScene.renderTextBlock(text, 6);
		block.maxWidth(WIDTH);
		block.setPos(0, pos);
		add(block);
		pos = block.bottom() + 6;
	}

	private void addButton( RedButton btn ){
		add(btn);
		btn.setRect(0, pos, WIDTH, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;
	}

	private void reopen(){
		hide();
		Game.scene().addToFront(new WndLiveLink());
	}
}
