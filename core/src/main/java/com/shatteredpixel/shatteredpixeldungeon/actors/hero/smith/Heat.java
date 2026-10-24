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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//The Smith's weapon heat, 0-100. Melee hits raise it, it slowly fades while not fighting,
// and stepping into water quenches it all at once (see Smithing.quench).
public class Heat extends Buff {

	public static final float GLOWING   = 40;
	public static final float OVERHEAT  = 80;
	public static final float MAX       = 100;

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	private float heat = 0;
	private float lastHit = 0;

	public float heat(){
		return heat;
	}

	public boolean glowing(){
		return heat >= GLOWING;
	}

	public boolean overheated(){
		return heat >= OVERHEAT;
	}

	public void add( float amount ){
		heat = Math.max(floor(), Math.min(MAX, heat + amount));
		if (amount > 0) lastHit = Actor.now();
		BuffIndicator.refreshHero();
	}

	public void set( float amount ){
		heat = Math.max(floor(), Math.min(MAX, amount));
		BuffIndicator.refreshHero();
	}

	//the Forge Lord's heat never fully goes out
	private float floor(){
		if (target instanceof Hero && Evolution.is((Hero) target, HeroSubClass.FLAMESMITH)){
			return 20f * ((Hero) target).pointsInTalent(Talent.ETERNAL_FLAME);
		}
		return 0;
	}

	@Override
	public boolean act() {
		if (target instanceof Hero){
			Hero hero = (Hero) target;
			int pos = hero.pos;
			if (Dungeon.level.water[pos] && !hero.flying && heat >= 20){
				Smithing.quench(hero, this);
			} else {
				Blob fire = Dungeon.level.blobs.get(Fire.class);
				if (fire != null && fire.volume > 0 && fire.cur[pos] > 0 && hero.hasTalent(Talent.FIRE_WALKER)){
					add(10);
					Smithing.heal(hero, hero.pointsInTalent(Talent.FIRE_WALKER));
				} else if (Actor.now() - lastHit > 2 && heat > floor()){
					heat = Math.max(floor(), heat - 3);
					BuffIndicator.refreshHero();
				}
			}
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return heat >= 1 ? BuffIndicator.FIRE : BuffIndicator.NONE;
	}

	@Override
	public void tintIcon(Image icon) {
		if (overheated())     icon.hardlight(1f, 0.3f, 0.1f);
		else if (glowing())   icon.hardlight(1f, 0.6f, 0.2f);
		else                  icon.hardlight(0.6f, 0.5f, 0.4f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - heat / MAX;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) heat);
	}

	@Override
	public String name() {
		if (overheated()) return Messages.get(this, "name_overheated");
		if (glowing())    return Messages.get(this, "name_glowing");
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		String state = overheated() ? "desc_overheated" : glowing() ? "desc_glowing" : "desc_cold";
		return Messages.get(this, "desc", (int) heat) + "\n\n" + Messages.get(this, state);
	}

	private static final String HEAT = "heat";
	private static final String LAST_HIT = "last_hit";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HEAT, heat);
		bundle.put(LAST_HIT, lastHit);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		heat = bundle.getFloat(HEAT);
		lastHit = bundle.getFloat(LAST_HIT);
	}
}
