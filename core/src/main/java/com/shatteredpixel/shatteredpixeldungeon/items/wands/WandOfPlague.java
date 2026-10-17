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

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Plague;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PoisonParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

//infects a target with plague, which spreads to its allies when it dies
public class WandOfPlague extends Wand {

	{
		image = ItemSpriteSheet.WAND_PLAGUE;
		collisionProperties = Ballistica.PROJECTILE;
	}

	private int duration(int lvl){
		return 6 + 2*lvl;
	}

	@Override
	public void onZap(Ballistica bolt) {
		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null){
			wandProc(ch, chargesPerCast());
			Buff.affect(ch, Plague.class).set(duration(buffedLvl()));
			CellEmitter.center(ch.pos).burst(PoisonParticle.SPLASH, 6);
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		MagicMissile.boltFromChar(curUser.sprite.parent, MagicMissile.POISON, curUser.sprite, bolt.collisionPos, callback);
		Sample.INSTANCE.play(Assets.Sounds.ZAP);
	}

	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		int level = Math.max(0, buffedLvl());
		float procChance = (level+1f)/(level+4f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance){
			Buff.affect(defender, Plague.class).set(3 + level);
		}
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color(0x88AA22);
		particle.am = 0.6f;
		particle.setLifespan(1.2f);
		particle.acc.set(0, 15);
		particle.setSize(0.5f, 2.5f);
		particle.shuffleXY(1f);
	}

	@Override
	public String statsDesc() {
		if (levelKnown) return Messages.get(this, "stats_desc", duration(buffedLvl()));
		else return Messages.get(this, "stats_desc", duration(0));
	}

	@Override
	public String upgradeStat1(int level) {
		return Integer.toString(duration(level));
	}
}
