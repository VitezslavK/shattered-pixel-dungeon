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
import java.util.Collections;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
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

	//contagion: from +2 the zap also infects nearby enemies, one more every 2 levels
	public static int contagion(int lvl){
		return lvl < 2 ? 0 : 1 + (lvl - 2) / 2;
	}

	//festering: from +3 the infected take more damage from everything else (10%, +5% per level, max 50%)
	public static float fester(int lvl){
		return lvl < 3 ? 0 : Math.min(0.5f, 0.10f + 0.05f * (lvl - 3));
	}

	@Override
	public void onZap(Ballistica bolt) {
		Char ch = Actor.findChar(bolt.collisionPos);
		if (ch != null){
			int lvl = buffedLvl();
			wandProc(ch, chargesPerCast());
			infect(ch, duration(lvl), fester(lvl));
			CellEmitter.center(ch.pos).burst(PoisonParticle.SPLASH, 6);

			//the plague leaps to the closest other enemies within 2 tiles
			int jumps = contagion(lvl);
			if (jumps > 0){
				ArrayList<Char> near = new ArrayList<>();
				for (Char other : Actor.chars()){
					if (other != ch && other != curUser && other.isAlive() && other.alignment == ch.alignment
							&& Dungeon.level.distance(other.pos, ch.pos) <= 2 && Dungeon.level.heroFOV[other.pos]){
						near.add(other);
					}
				}
				Collections.sort(near, (a, b) -> Dungeon.level.distance(a.pos, ch.pos) - Dungeon.level.distance(b.pos, ch.pos));
				for (int i = 0; i < Math.min(jumps, near.size()); i++){
					Char victim = near.get(i);
					infect(victim, duration(lvl) / 2f + 2, fester(lvl));
					if (victim.sprite != null && ch.sprite != null){
						curUser.sprite.parent.add(new Beam.LightRay(ch.sprite.center(), victim.sprite.center()));
						CellEmitter.center(victim.pos).burst(PoisonParticle.SPLASH, 4);
					}
				}
			}
		}
	}

	private static void infect(Char ch, float duration, float fester){
		Plague p = Buff.affect(ch, Plague.class);
		p.set(duration);
		p.setFester(fester);
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
		int lvl = levelKnown ? buffedLvl() : 0;
		String desc = Messages.get(this, "stats_desc", duration(lvl));
		if (contagion(lvl) > 0) desc += "\n\n" + Messages.get(this, "stats_contagion", contagion(lvl));
		if (fester(lvl) > 0) desc += "\n\n" + Messages.get(this, "stats_fester", Math.round(fester(lvl) * 100));
		if (lvl < 3) desc += "\n\n" + Messages.get(this, "stats_locked");
		return desc;
	}

	@Override
	public String upgradeStat1(int level) {
		return Integer.toString(duration(level));
	}

	@Override
	public String upgradeStat2(int level) {
		return Integer.toString(contagion(level));
	}

	@Override
	public String upgradeStat3(int level) {
		return Math.round(fester(level) * 100) + "%";
	}
}
