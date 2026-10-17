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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.online.GhostSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostHeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//The ghost of another player's hero (or one of your own past heroes) invading this run.
// Its strength scales with the current depth, its gear comes from the original hero.
public class GhostInvader extends Mob implements GhostHeroSprite.GhostHero {

	{
		spriteClass = GhostHeroSprite.class;
		maxLvl = Hero.MAX_LEVEL;
		properties.add(Property.MINIBOSS);
		properties.add(Property.UNDEAD);
		lootChance = 0;
	}

	private GhostSnapshot ghost = new GhostSnapshot();
	private boolean announced = false;

	public static GhostInvader create( GhostSnapshot ghost, int depth ){
		GhostInvader g = new GhostInvader();
		g.ghost = ghost;
		g.setupStats(depth);
		g.HP = g.HT;
		return g;
	}

	private void setupStats( int depth ){
		HT = 20 + 8*depth;
		defenseSkill = 6 + depth;
		EXP = 4 + depth/2;
	}

	@Override
	public String name() {
		return Messages.get(this, "name", ghost.playerName);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", ghost.playerName,
				Messages.titleCase(ghost.heroClass.title()), ghost.lvl, ghost.depth);
	}

	@Override
	public HeroClass ghostClass() {
		return ghost.heroClass;
	}

	@Override
	public int ghostArmorTier() {
		return ghost.armorTier();
	}

	@Override
	public boolean ghostHostile() {
		return true;
	}

	@Override
	public int attackSkill( Char target ) {
		return 12 + 2*Dungeon.scalingDepth();
	}

	@Override
	public int damageRoll() {
		int d = Dungeon.scalingDepth();
		int wl = Math.min(ghost.weaponLvl, 5);
		return Random.NormalIntRange(1 + d/3 + wl/2, 4 + d + ghost.weaponTier() + wl);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2*ghost.armorTier() + Math.min(ghost.armorLvl, 5));
	}

	@Override
	public void notice() {
		super.notice();
		if (!announced){
			announced = true;
			GLog.n(Messages.get(this, "invades", ghost.playerName));
		}
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		GLog.p(Messages.get(this, "defeated", ghost.playerName));

		//drops a copy of the ghost's weapon or armor, capped at +3
		Item reward = Random.Int(2) == 0 ? ghost.createWeapon() : ghost.createArmor();
		if (reward == null) reward = ghost.createWeapon();
		if (reward == null) reward = ghost.createArmor();
		if (reward instanceof MeleeWeapon){
			reward.level(Math.min(ghost.weaponLvl, 3));
		} else if (reward instanceof Armor){
			reward.level(Math.min(ghost.armorLvl, 3));
		}
		if (reward != null){
			reward.cursed = false;
			reward.identify();
			Dungeon.level.drop(reward, pos).sprite.drop();
		}
	}

	private static final String GHOST = "ghost";
	private static final String ANNOUNCED = "announced";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put(GHOST, ghost);
		bundle.put(ANNOUNCED, announced);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		ghost = (GhostSnapshot) bundle.get(GHOST);
		if (ghost == null) ghost = new GhostSnapshot();
		announced = bundle.getBoolean(ANNOUNCED);
	}
}
