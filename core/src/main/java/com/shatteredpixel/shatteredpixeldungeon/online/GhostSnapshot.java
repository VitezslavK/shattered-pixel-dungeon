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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.Reflection;

//A compact, shareable description of a hero at the end of a run. Used to create ghosts that
// invade other players' games or can be summoned as allies by friends.
public class GhostSnapshot implements Bundlable {

	public String playerName = "";
	public String friendCode = "";
	public HeroClass heroClass = HeroClass.WARRIOR;
	public HeroSubClass subClass = HeroSubClass.NONE;
	public boolean evolved = false;
	public int lvl = 1;
	public int depth = 1;
	public boolean won = false;
	//simple class names only, resolved against known packages when used
	public String weapon = "";
	public int weaponLvl = 0;
	public String armor = "";
	public int armorLvl = 0;

	public static GhostSnapshot fromHero( Hero hero, boolean won ){
		GhostSnapshot s = new GhostSnapshot();
		s.playerName = OnlineService.playerName();
		s.friendCode = OnlineService.friendCode();
		s.heroClass = hero.heroClass;
		s.subClass = hero.subClass;
		s.evolved = hero.evolved;
		s.lvl = hero.lvl;
		s.depth = Dungeon.depth;
		s.won = won;
		if (hero.belongings.weapon() instanceof MeleeWeapon){
			s.weapon = hero.belongings.weapon().getClass().getSimpleName();
			s.weaponLvl = Math.max(0, hero.belongings.weapon().buffedLvl());
		}
		if (hero.belongings.armor() != null){
			s.armor = hero.belongings.armor().getClass().getSimpleName();
			s.armorLvl = Math.max(0, hero.belongings.armor().buffedLvl());
		}
		return s;
	}

	//only classes from the game's own melee weapon package can be created from snapshots
	public MeleeWeapon createWeapon(){
		return createItem("com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.", weapon, MeleeWeapon.class);
	}

	//only classes from the game's own armor package can be created from snapshots
	public Armor createArmor(){
		return createItem("com.shatteredpixel.shatteredpixeldungeon.items.armor.", armor, Armor.class);
	}

	private static <T> T createItem( String pkg, String simpleName, Class<T> type ){
		if (simpleName == null || !simpleName.matches("[A-Za-z]{1,40}")) return null;
		try {
			//remote data may name classes that don't exist, which isn't worth reporting
			Class<?> cls = Reflection.forNameUnhandled(pkg + simpleName);
			if (cls == null || !type.isAssignableFrom(cls)) return null;
			Object o = Reflection.newInstanceUnhandled(cls);
			return type.isInstance(o) ? type.cast(o) : null;
		} catch (Exception e){
			return null;
		}
	}

	//tiers are cached, as they are checked every time a ghost attacks or defends
	private int armorTierCache = -1;
	private int weaponTierCache = -1;

	public int armorTier(){
		if (armorTierCache == -1){
			Armor a = createArmor();
			armorTierCache = a == null ? 0 : a.tier;
		}
		return armorTierCache;
	}

	public int weaponTier(){
		if (weaponTierCache == -1){
			MeleeWeapon w = createWeapon();
			weaponTierCache = w == null ? 1 : w.tier;
		}
		return weaponTierCache;
	}

	public String toJson(){
		return "{"
				+ "\"player_name\":" + quote(playerName) + ","
				+ "\"friend_code\":" + quote(friendCode) + ","
				+ "\"hero_class\":" + quote(heroClass.name()) + ","
				+ "\"sub_class\":" + quote(subClass.name()) + ","
				+ "\"evolved\":" + evolved + ","
				+ "\"lvl\":" + lvl + ","
				+ "\"depth\":" + depth + ","
				+ "\"won\":" + won + ","
				+ "\"weapon\":" + quote(weapon) + ","
				+ "\"weapon_lvl\":" + weaponLvl + ","
				+ "\"armor\":" + quote(armor) + ","
				+ "\"armor_lvl\":" + armorLvl
				+ "}";
	}

	private static String quote( String s ){
		if (s == null) s = "";
		StringBuilder b = new StringBuilder("\"");
		for (char c : s.toCharArray()){
			if (c == '"' || c == '\\') b.append('\\');
			if (c >= 32) b.append(c);
		}
		return b.append('"').toString();
	}

	//parses a row from the server, returns null if the data is invalid
	public static GhostSnapshot fromJson( JsonValue v ){
		try {
			GhostSnapshot s = new GhostSnapshot();
			s.playerName = sanitize(v.getString("player_name", "???"), 16);
			s.friendCode = sanitize(v.getString("friend_code", ""), 8);
			s.heroClass = HeroClass.valueOf(v.getString("hero_class", "WARRIOR"));
			s.subClass = HeroSubClass.valueOf(v.getString("sub_class", "NONE"));
			s.evolved = v.getBoolean("evolved", false);
			s.lvl = (int)GameMath.gate(1, v.getInt("lvl", 1), 36);
			s.depth = (int)GameMath.gate(1, v.getInt("depth", 1), 30);
			s.won = v.getBoolean("won", false);
			s.weapon = sanitize(v.getString("weapon", ""), 40);
			s.weaponLvl = (int)GameMath.gate(0, v.getInt("weapon_lvl", 0), 15);
			s.armor = sanitize(v.getString("armor", ""), 40);
			s.armorLvl = (int)GameMath.gate(0, v.getInt("armor_lvl", 0), 15);
			return s;
		} catch (Exception e){
			return null;
		}
	}

	private static String sanitize( String s, int max ){
		if (s == null) return "";
		s = s.replaceAll("[^\\p{L}\\p{N} _-]", "");
		return s.length() > max ? s.substring(0, max) : s;
	}

	private static final String NAME = "name", CODE = "code", CLS = "cls", SUB = "sub", EVO = "evo",
			LVL = "lvl", DEPTH = "depth", WON = "won", WEP = "wep", WEP_LVL = "wep_lvl", ARM = "arm", ARM_LVL = "arm_lvl";

	@Override
	public void storeInBundle(Bundle bundle) {
		bundle.put(NAME, playerName);
		bundle.put(CODE, friendCode);
		bundle.put(CLS, heroClass);
		bundle.put(SUB, subClass);
		bundle.put(EVO, evolved);
		bundle.put(LVL, lvl);
		bundle.put(DEPTH, depth);
		bundle.put(WON, won);
		bundle.put(WEP, weapon);
		bundle.put(WEP_LVL, weaponLvl);
		bundle.put(ARM, armor);
		bundle.put(ARM_LVL, armorLvl);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		playerName = bundle.getString(NAME);
		friendCode = bundle.getString(CODE);
		heroClass = bundle.getEnum(CLS, HeroClass.class);
		subClass = bundle.getEnum(SUB, HeroSubClass.class);
		evolved = bundle.getBoolean(EVO);
		lvl = bundle.getInt(LVL);
		depth = bundle.getInt(DEPTH);
		won = bundle.getBoolean(WON);
		weapon = bundle.getString(WEP);
		weaponLvl = bundle.getInt(WEP_LVL);
		armor = bundle.getString(ARM);
		armorLvl = bundle.getInt(ARM_LVL);
	}
}
