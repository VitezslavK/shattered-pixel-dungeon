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

package com.shatteredpixel.shatteredpixeldungeon.online;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashMap;

//Live link: friends start the same dungeon (the room code doubles as the seed) at the same time.
// Every player posts small events about their run, and everyone's game reads the room's new events
// every few seconds. That shows each other's progress live, announces bosses and deaths, and lets
// players send each other one gift per floor: help, or some mischief.
public class LiveLink {

	private static final String ROOM_KEY = "live_room";
	private static final String LAST_ID_KEY = "live_last_id";
	private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

	public static class Friend {
		public String code;
		public String name;
		public HeroClass heroClass = HeroClass.WARRIOR;
		public int depth, lvl, hp, ht;
		public boolean dead, won;
		public long seenAt;
	}

	private static final LinkedHashMap<String, Friend> friends = new LinkedHashMap<>();
	private static final ArrayList<String[]> pendingGifts = new ArrayList<>();

	private static boolean polling = false;
	private static boolean loadedOnce = false;
	private static float pollTimer = 0;

	private static long runKey = Long.MIN_VALUE;
	private static int lastDepth = -1, lastLvl = -1;
	private static float lastHpFrac = 1f;
	private static long lastStatusMs = 0;
	private static boolean lastLocked = false;
	private static int giftDepth = -1;

	public static String room(){
		return SPDSettings.getString(ROOM_KEY, "", 16);
	}

	public static boolean inRoom(){
		return !room().isEmpty();
	}

	//true while playing the run that belongs to the current room
	public static boolean activeRun(){
		return inRoom() && OnlineService.enabled() && Dungeon.hero != null
				&& !Dungeon.daily && room().equals(Dungeon.customSeedText);
	}

	public static String createRoom(){
		StringBuilder b = new StringBuilder("LIVE-");
		for (int i = 0; i < 6; i++) b.append(CODE_CHARS.charAt(Random.Int(CODE_CHARS.length())));
		setRoom(b.toString());
		return room();
	}

	//accepts "ABC123" or "LIVE-ABC123", returns false if the code isn't valid
	public static boolean joinRoom( String input ){
		String code = input == null ? "" : input.toUpperCase().replaceAll("[^A-Z0-9]", "");
		if (code.startsWith("LIVE")) code = code.substring(4);
		if (!code.matches("[A-Z0-9]{6}")) return false;
		setRoom("LIVE-" + code);
		return true;
	}

	private static void setRoom( String room ){
		SPDSettings.put(ROOM_KEY, room);
		SPDSettings.put(LAST_ID_KEY, 0L);
		//the room code is also the dungeon seed, so everyone plays the same dungeon
		SPDSettings.customSeed(room);
		reset();
	}

	public static void leave(){
		if (SPDSettings.customSeed().equals(room())) SPDSettings.customSeed("");
		SPDSettings.put(ROOM_KEY, "");
		reset();
	}

	private static void reset(){
		synchronized (friends){ friends.clear(); }
		synchronized (pendingGifts){ pendingGifts.clear(); }
		loadedOnce = false;
		runKey = Long.MIN_VALUE;
		giftDepth = -1;
	}

	public static ArrayList<Friend> friends(){
		synchronized (friends){
			return new ArrayList<>(friends.values());
		}
	}

	//------------------------------ reading the room ------------------------------

	//called every frame from GameScene.update (render thread)
	public static void update( float elapsed ){
		if (!inRoom() || !OnlineService.enabled()) return;
		pollTimer += elapsed;
		if (pollTimer >= 6f){
			pollTimer = 0;
			poll(null);
		}
	}

	//fetches the room's new events, the callback runs on the render thread afterwards
	public static void poll( Runnable onDone ){
		if (!inRoom() || !OnlineService.enabled() || polling){
			if (onDone != null) onDone.run();
			return;
		}
		polling = true;
		final String room = room();
		final long savedLastId = SPDSettings.getLong(LAST_ID_KEY, 0);
		final boolean first = !loadedOnce;
		String path = "live_events?select=*&room=eq." + room;
		//the first read after starting rebuilds everyone's status from recent history
		path += first ? "&order=id.desc&limit=150" : "&id=gt." + savedLastId + "&order=id.asc&limit=100";
		OnlineService.get(path, new OnlineService.RawCallback() {
			@Override
			public void onResult( com.badlogic.gdx.utils.JsonValue arr ) {
				polling = false;
				if (arr != null && room.equals(room())){
					ArrayList<com.badlogic.gdx.utils.JsonValue> rows = new ArrayList<>();
					for (com.badlogic.gdx.utils.JsonValue v = arr.child; v != null; v = v.next) rows.add(v);
					if (first) java.util.Collections.reverse(rows);
					long maxId = savedLastId;
					for (com.badlogic.gdx.utils.JsonValue v : rows){
						long id = v.getLong("id", 0);
						//events from before this device last read the room only update statuses
						process(v, id > savedLastId && (savedLastId > 0 || !first));
						maxId = Math.max(maxId, id);
					}
					SPDSettings.put(LAST_ID_KEY, maxId);
					loadedOnce = true;
				}
				if (onDone != null) onDone.run();
			}
		});
	}

	private static void process( com.badlogic.gdx.utils.JsonValue v, boolean fresh ){
		String code = v.getString("friend_code", "");
		if (code.equals(OnlineService.friendCode())) return;
		String kind = v.getString("kind", "");
		Friend f;
		synchronized (friends){
			f = friends.get(code);
			if (f == null){
				f = new Friend();
				f.code = code;
				friends.put(code, f);
			}
			f.name = v.getString("player_name", "???").replaceAll("[^\\p{L}\\p{N} _-]", "");
			try { f.heroClass = HeroClass.valueOf(v.getString("hero_class", "WARRIOR")); } catch (Exception e){ /* unknown class */ }
			f.depth = (int)GameMath.gate(0, v.getInt("depth", 1), Dungeon.LAST_DEPTH);
			f.lvl = (int)GameMath.gate(1, v.getInt("lvl", 1), 36);
			f.hp = Math.max(0, v.getInt("hp", 0));
			f.ht = Math.max(0, v.getInt("ht", 0));
			f.seenAt = System.currentTimeMillis();
			if (kind.equals("join")) { f.dead = false; f.won = false; }
			if (kind.equals("death")) f.dead = true;
			if (kind.equals("win")) f.won = true;
		}
		if (!fresh) return;
		switch (kind){
			case "join":
				GLog.i(Messages.get(LiveLink.class, "joined", f.name, Messages.titleCase(f.heroClass.title())));
				break;
			case "boss":
				GLog.i(Messages.get(LiveLink.class, "boss", f.name, f.depth));
				break;
			case "death":
				GLog.w(Messages.get(LiveLink.class, "died", f.name, f.depth));
				break;
			case "win":
				GLog.p(Messages.get(LiveLink.class, "won", f.name));
				break;
			case "gift":
				if (v.getString("target", "").equals(OnlineService.friendCode())){
					synchronized (pendingGifts){
						pendingGifts.add(new String[]{v.getString("gift", ""), f.name});
					}
				}
				break;
		}
	}

	//------------------------------ writing to the room ------------------------------

	private static void post( String kind, String target, String gift ){
		Hero hero = Dungeon.hero;
		String json = "{\"room\":\"" + room() + "\","
				+ "\"friend_code\":\"" + OnlineService.friendCode() + "\","
				+ "\"player_name\":\"" + OnlineService.playerName().replace("\"", "") + "\","
				+ "\"hero_class\":\"" + hero.heroClass.name() + "\","
				+ "\"kind\":\"" + kind + "\","
				+ "\"depth\":" + (int)GameMath.gate(0, Dungeon.depth, Dungeon.LAST_DEPTH) + ","
				+ "\"lvl\":" + (int)GameMath.gate(1, hero.lvl, 36) + ","
				+ "\"hp\":" + (int)GameMath.gate(0, hero.HP, 100000) + ","
				+ "\"ht\":" + (int)GameMath.gate(0, hero.HT, 100000) + ","
				+ "\"target\":\"" + target + "\","
				+ "\"gift\":\"" + gift + "\"}";
		OnlineService.post("live_events", json);
	}

	//called from Hero.act once per hero action (actor thread)
	public static void tick( Hero hero ){
		if (!activeRun()) return;

		long key = Dungeon.seed ^ Dungeon.hero.heroClass.ordinal();
		if (key != runKey){
			runKey = key;
			lastDepth = Dungeon.depth;
			lastLvl = hero.lvl;
			lastStatusMs = System.currentTimeMillis();
			lastLocked = Dungeon.level.locked;
			post("join", "", "");
		}

		long now = System.currentTimeMillis();
		float hpFrac = hero.HT > 0 ? hero.HP / (float)hero.HT : 0;
		if (now - lastStatusMs > 4000 && (Dungeon.depth != lastDepth || hero.lvl != lastLvl
				|| Math.abs(hpFrac - lastHpFrac) > 0.25f || now - lastStatusMs > 45000)){
			lastDepth = Dungeon.depth;
			lastLvl = hero.lvl;
			lastHpFrac = hpFrac;
			lastStatusMs = now;
			post("status", "", "");
		}

		//a boss level unlocking means its boss was beaten
		boolean locked = Dungeon.level.locked;
		if (lastLocked && !locked && Dungeon.bossLevel()){
			post("boss", "", "");
		}
		lastLocked = locked;

		applyGifts(hero);
	}

	//called when the run ends, from GhostPool.onRunEnd
	public static void onRunEnd( boolean won ){
		if (activeRun()) post(won ? "win" : "death", "", "");
	}

	//------------------------------ gifts ------------------------------

	public static boolean canGift(){
		return activeRun() && Dungeon.hero.isAlive() && giftDepth != Dungeon.depth;
	}

	public static boolean sendGift( Friend friend, boolean help ){
		if (!canGift() || friend == null || friend.code == null || !friend.code.matches("[A-Z0-9]{6}")) return false;
		giftDepth = Dungeon.depth;
		post("gift", friend.code, help ? "help" : "mischief");
		return true;
	}

	private static void applyGifts( Hero hero ){
		ArrayList<String[]> gifts;
		synchronized (pendingGifts){
			if (pendingGifts.isEmpty()) return;
			gifts = new ArrayList<>(pendingGifts);
			pendingGifts.clear();
		}
		for (String[] g : gifts){
			if ("help".equals(g[0])){
				int heal = Math.min(Math.round(hero.HT * 0.3f), hero.HT - hero.HP);
				hero.HP += heal;
				int shield = Math.round(hero.HT * 0.1f) + 2;
				Buff.affect(hero, Barrier.class).incShield(shield);
				if (hero.sprite != null){
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(Math.max(heal, shield)), FloatingText.HEALING);
					hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 6);
				}
				GLog.p(Messages.get(LiveLink.class, "got_help", g[1]));
			} else if ("mischief".equals(g[0])){
				if (Dungeon.bossLevel() || Dungeon.level.locked || !spawnMischief(hero)){
					GLog.i(Messages.get(LiveLink.class, "mischief_fizzled", g[1]));
				} else {
					GLog.n(Messages.get(LiveLink.class, "got_mischief", g[1]));
				}
			}
		}
	}

	//a monster of the hero's depth appears a short distance away and comes for them
	private static boolean spawnMischief( Hero hero ){
		Mob mob = Dungeon.level.createMob();
		if (mob == null) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int i = 0; i < Dungeon.level.length(); i++){
			int dist = Dungeon.level.distance(i, hero.pos);
			if (dist >= 4 && dist <= 8 && Dungeon.level.passable[i] && !Dungeon.level.solid[i]
					&& Actor.findChar(i) == null && Dungeon.level.openSpace[i]
					&& (!Char.hasProp(mob, Char.Property.LARGE) || Dungeon.level.openSpace[i])){
				cells.add(i);
			}
		}
		if (cells.isEmpty()) return false;
		mob.pos = Random.element(cells);
		mob.state = mob.HUNTING;
		GameScene.add(mob);
		//noticing the hero shows an alert above the monster, which needs its sprite
		if (mob.sprite != null) mob.beckon(hero.pos);
		return true;
	}
}
