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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.GhostAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.online.GhostPool;
import com.shatteredpixel.shatteredpixeldungeon.online.GhostSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.online.OnlineService;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//Every hero starts with a spirit bell. Once per run it summons the ghost of a friend's hero
// (chosen by their friend code) as an ally, or a random ghost if no friend is set or found.
public class SpiritBell extends Item {

	public static final String AC_RING     = "RING";
	public static final String AC_FRIEND   = "FRIEND";
	public static final String AC_NAME     = "NAME";

	{
		image = ItemSpriteSheet.SPIRIT_BELL;
		defaultAction = AC_RING;
		unique = true;
		bones = false;
	}

	private boolean used = false;
	private boolean waiting = false;

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.remove(AC_DROP);
		actions.remove(AC_THROW);
		if (!used) actions.add(AC_RING);
		actions.add(AC_FRIEND);
		actions.add(AC_NAME);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_RING)){
			if (used){
				GLog.w(Messages.get(this, "used"));
			} else if (waiting){
				GLog.i(Messages.get(this, "waiting"));
			} else {
				ring(hero);
			}
		} else if (action.equals(AC_FRIEND)){
			GameScene.show(new WndTextInput(Messages.get(this, "friend_title"),
					Messages.get(this, "friend_body", OnlineService.friendCode()),
					OnlineService.friendTarget(), 6, false,
					Messages.get(this, "confirm"), Messages.get(this, "cancel")){
				@Override
				public void onSelect(boolean positive, String text) {
					if (positive) OnlineService.friendTarget(text);
				}
			});
		} else if (action.equals(AC_NAME)){
			GameScene.show(new WndTextInput(Messages.get(this, "name_title"),
					Messages.get(this, "name_body"),
					OnlineService.playerName(), 16, false,
					Messages.get(this, "confirm"), Messages.get(this, "cancel")){
				@Override
				public void onSelect(boolean positive, String text) {
					if (positive) OnlineService.playerName(text);
				}
			});
		}
	}

	private void ring( Hero hero ){
		waiting = true;
		Sample.INSTANCE.play(Assets.Sounds.GHOST);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
		GLog.i(Messages.get(this, "ringing"));

		String friend = OnlineService.friendTarget();
		if (!friend.isEmpty() && OnlineService.enabled()){
			OnlineService.fetchFriend(friend, new OnlineService.Callback() {
				@Override
				public void onResult(ArrayList<GhostSnapshot> ghosts) {
					summon(ghosts.isEmpty() ? GhostPool.pick(1) : ghosts.get(0));
				}
			});
		} else {
			summon(GhostPool.pick(1));
		}
	}

	private void summon( GhostSnapshot ghost ){
		waiting = false;
		//the hero may have left the game while the request was running
		if (Dungeon.hero == null || Dungeon.level == null || !Dungeon.hero.isAlive()) return;
		if (ghost == null){
			GLog.w(Messages.get(this, "no_answer"));
			return;
		}

		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = Dungeon.hero.pos + i;
			if (Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		GhostAlly ally = GhostAlly.create(ghost);
		ally.pos = Random.element(cells);
		GameScene.add(ally);
		ScrollOfTeleportation.appear(ally, ally.pos);
		used = true;
		updateQuickslot();
		GLog.p(Messages.get(this, "answered", ghost.playerName));
	}

	@Override
	public String desc() {
		String desc = super.desc();
		desc += "\n\n" + Messages.get(this, "desc_code", OnlineService.friendCode(), OnlineService.playerName());
		String friend = OnlineService.friendTarget();
		desc += "\n" + (friend.isEmpty() ? Messages.get(this, "desc_no_friend") : Messages.get(this, "desc_friend", friend));
		desc += "\n\n" + Messages.get(this, OnlineService.enabled() ? "desc_online" : "desc_offline");
		if (used) desc += "\n\n" + Messages.get(this, "desc_used");
		return desc;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	private static final String USED = "used";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(USED, used);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		used = bundle.getBoolean(USED);
	}
}
