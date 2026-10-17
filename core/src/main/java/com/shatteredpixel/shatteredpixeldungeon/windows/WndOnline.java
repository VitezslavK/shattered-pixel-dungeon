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
import com.shatteredpixel.shatteredpixeldungeon.online.GhostSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.online.OnlineExtras;
import com.shatteredpixel.shatteredpixeldungeon.online.OnlineService;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Game;

import java.util.ArrayList;

//Everything about playing with other people in one place: your name and code, your friend's code,
// the daily leaderboard, who played recently and a short explanation of how it all works.
// Available from the title screen and from the in-game menu.
public class WndOnline extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final int GAP        = 2;

	private float pos;

	public WndOnline() {
		super();

		IconTitle title = new IconTitle(new ItemSprite(ItemSpriteSheet.SPIRIT_BELL), Messages.get(this, "title"));
		title.setRect(0, 0, WIDTH, 0);
		add(title);
		pos = title.bottom() + 4;

		String friend = OnlineService.friendTarget();
		String status = "";
		if (OnlineService.enabled()){
			if (OnlineService.lastError != null) status = "\n" + Messages.get(this, "status_error", OnlineService.lastError);
			else if (OnlineService.lastSuccess > 0) status = "\n" + Messages.get(this, "status_ok");
		}
		String info = Messages.get(this, OnlineService.enabled() ? "online" : "offline") + status
				+ "\n\n" + Messages.get(this, "your_name", OnlineService.playerName())
				+ (OnlineService.playerName().equals("Hero") ? " " + Messages.get(this, "name_default") : "")
				+ "\n" + Messages.get(this, "your_code", OnlineService.friendCode())
				+ "\n" + (friend.isEmpty() ? Messages.get(this, "no_friend") : Messages.get(this, "friend", friend));
		RenderedTextBlock text = PixelScene.renderTextBlock(info, 6);
		text.maxWidth(WIDTH);
		text.setPos(0, pos);
		add(text);
		pos = text.bottom() + 6;

		addButton(new RedButton(Messages.get(this, "set_name")) {
			@Override
			protected void onClick() {
				show(new WndTextInput(Messages.get(WndOnline.class, "name_title"),
						Messages.get(WndOnline.class, "name_body"),
						OnlineService.playerName(), 16, false,
						Messages.get(WndOnline.class, "confirm"), Messages.get(WndOnline.class, "cancel")){
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive) OnlineService.playerName(text);
						reopen();
					}
				});
			}
		});

		addButton(new RedButton(Messages.get(this, "set_friend")) {
			@Override
			protected void onClick() {
				show(new WndTextInput(Messages.get(WndOnline.class, "friend_title"),
						Messages.get(WndOnline.class, "friend_body", OnlineService.friendCode()),
						OnlineService.friendTarget(), 6, false,
						Messages.get(WndOnline.class, "confirm"), Messages.get(WndOnline.class, "cancel")){
					@Override
					public void onSelect(boolean positive, String text) {
						if (positive) OnlineService.friendTarget(text);
						reopen();
					}
				});
			}
		});

		if (OnlineService.enabled()) {
			RedButton live = new RedButton(Messages.get(this, "live")) {
				@Override
				protected void onClick() {
					hide();
					WndLiveLink.open();
				}
			};
			live.textColor(Window.TITLE_COLOR);
			addButton(live);

			addButton(new RedButton(Messages.get(this, "scores")) {
				@Override
				protected void onClick() {
					showScores();
				}
			});

			addButton(new RedButton(Messages.get(this, "players")) {
				@Override
				protected void onClick() {
					showPlayers();
				}
			});
		}

		RedButton help = new RedButton(Messages.get(this, "how")) {
			@Override
			protected void onClick() {
				show(new WndTitledMessage(Icons.get(Icons.INFO), Messages.get(WndOnline.class, "how"),
						Messages.get(WndOnline.class, "how_body")));
			}
		};
		help.textColor(Window.TITLE_COLOR);
		addButton(help);

		resize(WIDTH, (int)pos - GAP);
	}

	private void addButton( RedButton btn ){
		add(btn);
		btn.setRect(0, pos, WIDTH, BTN_HEIGHT);
		pos += BTN_HEIGHT + GAP;
	}

	private static void show( Window w ){
		Game.scene().addToFront(w);
	}

	private void reopen(){
		hide();
		show(new WndOnline());
	}

	private static void showScores(){
		final String day = OnlineExtras.utcDay(Game.realTime);
		OnlineExtras.fetchDailyTop(day, new OnlineExtras.ScoreCallback() {
			@Override
			public void onResult(ArrayList<OnlineExtras.Score> scores) {
				StringBuilder text = new StringBuilder();
				if (scores.isEmpty()){
					text.append(Messages.get(WndOnline.class, "scores_empty"));
				}
				int rank = 1;
				for (OnlineExtras.Score s : scores){
					text.append(rank++).append(". _").append(s.playerName).append("_ (")
							.append(Messages.titleCase(s.heroClass.title())).append(") ")
							.append(s.score).append(" - ")
							.append(s.won ? Messages.get(WndOnline.class, "victory") : Messages.get(WndOnline.class, "floor", s.depth))
							.append("\n");
				}
				show(new WndTitledMessage(Icons.get(Icons.RANKINGS),
						Messages.get(WndOnline.class, "scores_title", day), text.toString()));
			}
		});
	}

	private static void showPlayers(){
		OnlineService.fetchRecent(new OnlineService.Callback() {
			@Override
			public void onResult(ArrayList<GhostSnapshot> ghosts) {
				StringBuilder text = new StringBuilder();
				if (ghosts.isEmpty()){
					text.append(Messages.get(WndOnline.class, "players_empty"));
				}
				String mine = OnlineService.friendCode();
				int shown = 0;
				for (GhostSnapshot g : ghosts){
					if (shown++ >= 15) break;
					text.append("_").append(g.playerName).append("_ ")
							.append(g.friendCode.equals(mine) ? Messages.get(WndOnline.class, "you") + " " : "")
							.append("(").append(g.friendCode).append(") - ")
							.append(Messages.titleCase(g.heroClass.title())).append(" ")
							.append(Messages.get(WndOnline.class, "level", g.lvl)).append(", ")
							.append(g.won ? Messages.get(WndOnline.class, "victory") : Messages.get(WndOnline.class, "floor", g.depth))
							.append("\n");
				}
				show(new WndTitledMessage(Icons.get(Icons.TALENT), Messages.get(WndOnline.class, "players"), text.toString()));
			}
		});
	}
}
