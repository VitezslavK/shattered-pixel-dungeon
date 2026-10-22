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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

//a disease that deals steady damage and spreads to nearby characters of the same allegiance
// when its host dies
public class Plague extends Buff implements Hero.Doom {

	protected float left;
	//extra damage the infected takes from other sources, set by a high level wand of plague
	protected float fester = 0;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public void set( float duration ) {
		this.left = Math.max(duration, left);
	}

	public void setFester( float bonus ) {
		this.fester = Math.max(bonus, fester);
	}

	public float fester(){
		return fester;
	}

	//damage scales gently with depth
	private int damagePerTurn(){
		return 1 + Dungeon.scalingDepth()/8;
	}

	@Override
	public boolean attachTo(Char target) {
		if (super.attachTo(target) && target.sprite != null){
			CellEmitter.center(target.pos).burst( PoisonParticle.SPLASH, 4 );
			return true;
		}
		return false;
	}

	@Override
	public boolean act() {
		if (target.isAlive()) {
			target.damage( damagePerTurn(), this );
			spend( TICK );
			if ((left -= TICK) <= 0) {
				detach();
			}
		} else {
			detach();
		}
		return true;
	}

	//called from Char.die, the plague jumps to adjacent characters of the same alignment
	public void spread(){
		for (int i : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar(target.pos + i);
			if (ch != null && ch.isAlive() && ch.alignment == target.alignment){
				Plague p = Buff.affect(ch, Plague.class);
				p.set(Math.max(3, left));
				p.setFester(fester);
			}
		}
		if (target.sprite != null) CellEmitter.center(target.pos).burst( PoisonParticle.SPLASH, 8 );
	}

	@Override
	public int icon() {
		return BuffIndicator.POISON;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.5f, 0.7f, 0.1f);
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString((int) left);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", damagePerTurn(), dispTurns(left));
	}

	@Override
	public void onDeath() {
		Dungeon.fail( this );
		GLog.n( Messages.get(this, "ondeath") );
	}

	private static final String LEFT	= "left";
	private static final String FESTER	= "fester";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
		bundle.put( FESTER, fester );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		left = bundle.getFloat( LEFT );
		fester = bundle.getFloat( FESTER );
	}
}
