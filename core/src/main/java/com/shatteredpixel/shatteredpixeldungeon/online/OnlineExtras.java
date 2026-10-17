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

import com.badlogic.gdx.utils.JsonValue;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.GameMath;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

//daily challenge leaderboard and shared bones, on top of OnlineService
public class OnlineExtras {

	public static class Score {
		public String playerName;
		public HeroClass heroClass;
		public int score;
		public int depth;
		public boolean won;
	}

	public interface ScoreCallback {
		void onResult( ArrayList<Score> scores );
	}

	public static void uploadDailyScore( String day, int score, int depth, boolean won, HeroClass cls ){
		if (!day.matches("\\d{4}-\\d{2}-\\d{2}")) return;
		String json = "{\"day\":\"" + day + "\","
				+ "\"player_name\":\"" + OnlineService.playerName().replace("\"", "") + "\","
				+ "\"friend_code\":\"" + OnlineService.friendCode() + "\","
				+ "\"hero_class\":\"" + cls.name() + "\","
				+ "\"score\":" + Math.max(0, score) + ","
				+ "\"depth\":" + (int)GameMath.gate(1, depth, 31) + ","
				+ "\"won\":" + won + "}";
		OnlineService.post("daily_scores", json);
	}

	public static void fetchDailyTop( String day, ScoreCallback callback ){
		if (!day.matches("\\d{4}-\\d{2}-\\d{2}")){
			callback.onResult(new ArrayList<>());
			return;
		}
		OnlineService.get("daily_scores?select=*&day=eq." + day + "&order=score.desc&limit=10", new OnlineService.RawCallback() {
			@Override
			public void onResult(JsonValue arr) {
				ArrayList<Score> result = new ArrayList<>();
				if (arr != null){
					for (JsonValue v = arr.child; v != null; v = v.next){
						try {
							Score s = new Score();
							s.playerName = v.getString("player_name", "???").replaceAll("[^\\p{L}\\p{N} _-]", "");
							s.heroClass = HeroClass.valueOf(v.getString("hero_class", "WARRIOR"));
							s.score = Math.max(0, v.getInt("score", 0));
							s.depth = (int)GameMath.gate(1, v.getInt("depth", 1), 31);
							s.won = v.getBoolean("won", false);
							result.add(s);
						} catch (Exception e){
							//skip invalid rows
						}
					}
				}
				callback.onResult(result);
			}
		});
	}

	//one item a dead hero leaves behind for other players to find
	public static class BonesEntry {
		public int depth;
		public String category;
		public String item;
		public int lvl;

		public Item create(){
			String pkg;
			Class<?> type;
			switch (category == null ? "" : category){
				case "weapon": pkg = "com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee."; type = MeleeWeapon.class; break;
				case "armor":  pkg = "com.shatteredpixel.shatteredpixeldungeon.items.armor."; type = Armor.class; break;
				case "wand":   pkg = "com.shatteredpixel.shatteredpixeldungeon.items.wands."; type = Wand.class; break;
				case "ring":   pkg = "com.shatteredpixel.shatteredpixeldungeon.items.rings."; type = Ring.class; break;
				default: return null;
			}
			if (item == null || !item.matches("[A-Za-z]{1,40}")) return null;
			try {
				Class<?> cls = Reflection.forNameUnhandled(pkg + item);
				if (cls == null || !type.isAssignableFrom(cls)
						|| java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) return null;
				Item i = (Item) Reflection.newInstanceUnhandled(cls);
				if (i == null) return null;
				i.level(Math.min(lvl, 3));
				i.cursed = Random.Int(4) == 0;
				return i;
			} catch (Exception e){
				return null;
			}
		}
	}

	public static void uploadBones( Hero hero ){
		ArrayList<Item> candidates = new ArrayList<>();
		if (hero.belongings.weapon() instanceof MeleeWeapon) candidates.add(hero.belongings.weapon());
		if (hero.belongings.armor() != null) candidates.add(hero.belongings.armor());
		for (Item i : hero.belongings.backpack.items){
			if ((i instanceof Wand || i instanceof Ring) && !i.unique) candidates.add(i);
		}
		if (candidates.isEmpty()) return;
		Item it = Random.element(candidates);
		String category = it instanceof MeleeWeapon ? "weapon" : it instanceof Armor ? "armor" : it instanceof Wand ? "wand" : "ring";
		String json = "{\"depth\":" + (int)GameMath.gate(1, Dungeon.depth, 31) + ","
				+ "\"category\":\"" + category + "\","
				+ "\"item\":\"" + it.getClass().getSimpleName() + "\","
				+ "\"lvl\":" + (int)GameMath.gate(0, it.level(), 15) + "}";
		OnlineService.post("bones", json);
	}

	public interface BonesCallback {
		void onResult( ArrayList<BonesEntry> bones );
	}

	public static void fetchBones( BonesCallback callback ){
		OnlineService.get("bones?select=*&order=created_at.desc&limit=40", new OnlineService.RawCallback() {
			@Override
			public void onResult(JsonValue arr) {
				ArrayList<BonesEntry> result = new ArrayList<>();
				if (arr != null){
					for (JsonValue v = arr.child; v != null; v = v.next){
						BonesEntry b = new BonesEntry();
						b.depth = (int)GameMath.gate(1, v.getInt("depth", 1), 31);
						b.category = v.getString("category", "");
						b.item = v.getString("item", "");
						b.lvl = (int)GameMath.gate(0, v.getInt("lvl", 0), 15);
						result.add(b);
					}
				}
				callback.onResult(result);
			}
		});
	}
}
