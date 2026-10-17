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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.online.GhostSnapshot;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostHeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//a friend's hero summoned with the spirit bell, fights alongside the player for the rest of the run
public class GhostAlly extends DirectableAlly implements GhostHeroSprite.GhostHero {

	{
		spriteClass = GhostHeroSprite.class;
		immunities.add(AllyBuff.class);
		properties.add(Property.UNDEAD);
	}

	private GhostSnapshot ghost = new GhostSnapshot();

	public static GhostAlly create( GhostSnapshot ghost ){
		GhostAlly a = new GhostAlly();
		a.ghost = ghost;
		a.updateStats();
		a.HP = a.HT;
		return a;
	}

	private int heroLvl(){
		return Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
	}

	private void updateStats(){
		int newHT = 20 + 6*heroLvl();
		if (newHT > HT) HP += newHT - HT;
		HT = newHT;
		HP = Math.min(HP, HT);
		defenseSkill = heroLvl() + 4;
	}

	@Override
	protected boolean act() {
		updateStats();
		return super.act();
	}

	@Override
	public String name() {
		return Messages.get(this, "name", ghost.playerName);
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", ghost.playerName, Messages.titleCase(ghost.heroClass.title()));
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
		return false;
	}

	@Override
	public int attackSkill( Char target ) {
		return heroLvl() + 10;
	}

	@Override
	public int damageRoll() {
		int lvl = heroLvl();
		int wl = Math.min(ghost.weaponLvl, 5);
		return Random.NormalIntRange(1 + lvl/3 + wl/2, 4 + lvl + ghost.weaponTier() + wl);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2*ghost.armorTier() + Math.min(ghost.armorLvl, 5));
	}

	@Override
	public float speed() {
		float speed = super.speed();
		if (state == WANDERING && defendingPos == -1 && Dungeon.level.distance(pos, Dungeon.hero.pos) > 1){
			speed *= 2;
		}
		return speed;
	}

	@Override
	public void defendPos(int cell) {
		GLog.i(Messages.get(GhostAlly.class, "direct_defend"));
		super.defendPos(cell);
	}

	@Override
	public void followHero() {
		GLog.i(Messages.get(GhostAlly.class, "direct_follow"));
		super.followHero();
	}

	@Override
	public void targetChar(Char ch) {
		GLog.i(Messages.get(GhostAlly.class, "direct_attack"));
		super.targetChar(ch);
	}

	private static final String GHOST = "ghost";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put(GHOST, ghost);
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		ghost = (GhostSnapshot) bundle.get(GHOST);
		if (ghost == null) ghost = new GhostSnapshot();
	}
}
