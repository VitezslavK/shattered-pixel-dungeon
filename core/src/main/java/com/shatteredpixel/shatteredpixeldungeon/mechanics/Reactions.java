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

package com.shatteredpixel.shatteredpixeldungeon.mechanics;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Plague;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.journal.Discoveries;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

//Elemental reactions: combining certain conditions, damage types and terrain produces extra effects.
// The game doesn't explain them up front, each one is recorded in the discovery codex the
// first time the player sees it happen.
public class Reactions {

	public enum Reaction {
		CONDUCTION,     //electricity + water
		SHATTER,        //frozen + physical hit
		FLASH_FREEZE,   //chill + water
		STEAM_BURST,    //fire + chill/frost
		CLEANSING_FIRE, //fire + plague
		TOXIC_IGNITION, //fire + toxic gas
		CAUSTIC_BLOOD;  //corrosion + bleeding

		public String title(){
			return Messages.get(Reactions.class, name().toLowerCase() + "_title");
		}

		public String desc(){
			return Messages.get(Reactions.class, name().toLowerCase() + "_desc");
		}

		public String hint(){
			return Messages.get(Reactions.class, name().toLowerCase() + "_hint");
		}
	}

	//damage dealt by reactions themselves, which never triggers further reactions
	public static class ReactionDamage {}

	//prevents reactions from triggering each other in a loop
	private static boolean reacting = false;
	//set while characters are being restored from a save
	public static boolean suspended = false;

	private static void trigger( Reaction r, Char ch ){
		if (ch.sprite != null && Dungeon.level.heroFOV[ch.pos]){
			ch.sprite.showStatus(CharSprite.WARNING, r.title());
			Discoveries.discover(r);
		}
	}

	private static boolean wet( Char ch ){
		return !ch.flying && Dungeon.level.water[ch.pos];
	}

	private static boolean electric( Object src ){
		return src instanceof Electricity || src instanceof Shocking || src instanceof WandOfLightning;
	}

	//called from Char.damage, before damage is applied
	public static int onDamage( Char ch, int dmg, Object src ){
		if (reacting || suspended || dmg <= 0 || src instanceof ReactionDamage || Dungeon.level == null) return dmg;
		reacting = true;
		try {
			if (electric(src) && wet(ch)){
				dmg = Math.round(dmg * 1.3f);
				trigger(Reaction.CONDUCTION, ch);
				//the current flows through the water to nearby characters
				int spread = Math.max(1, dmg/3);
				ArrayList<Char> others = new ArrayList<>();
				for (Char other : Actor.chars()){
					if (other != ch && wet(other) && Dungeon.level.distance(ch.pos, other.pos) <= 2){
						others.add(other);
					}
				}
				for (Char other : others){
					other.damage(spread, new ReactionDamage());
					if (other.sprite != null) other.sprite.centerEmitter().burst(SparkParticle.FACTORY, 4);
				}
			}

			if (src instanceof Char && ch.buff(Frost.class) != null){
				dmg = Math.round(dmg * 1.5f);
				trigger(Reaction.SHATTER, ch);
				if (ch.sprite != null) ch.sprite.emitter().burst(Speck.factory(Speck.STEAM), 6);
			}
		} finally {
			reacting = false;
		}
		return dmg;
	}

	//called from Buff.attachTo after a buff is successfully added
	public static void onBuffAttached( Char ch, Buff buff ){
		if (reacting || suspended || Dungeon.level == null || Dungeon.hero == null || !ch.isAlive()) return;
		reacting = true;
		try {
			if (buff instanceof Chill && wet(ch) && ch.buff(Frost.class) == null){
				Buff.affect(ch, Frost.class, 2f);
				trigger(Reaction.FLASH_FREEZE, ch);

			} else if (buff instanceof Burning){
				if (ch.buff(Chill.class) != null || ch.buff(Frost.class) != null){
					Buff.detach(ch, Chill.class);
					Buff.detach(ch, Frost.class);
					ch.damage(2 + Dungeon.scalingDepth()/3, new ReactionDamage());
					if (ch.isAlive()) Buff.prolong(ch, Blindness.class, 2f);
					if (ch.sprite != null) CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 10);
					trigger(Reaction.STEAM_BURST, ch);
				}

				Plague plague = ch.buff(Plague.class);
				if (plague != null && ch.isAlive()){
					plague.detach();
					ch.damage(3 + Dungeon.scalingDepth()/4, new ReactionDamage());
					trigger(Reaction.CLEANSING_FIRE, ch);
				}

				Blob gas = Dungeon.level.blobs.get(ToxicGas.class);
				if (gas != null && gas.volume > 0 && gas.cur[ch.pos] > 0){
					int cell = ch.pos;
					gas.clear(cell);
					int dmg = 4 + Dungeon.scalingDepth()/2;
					trigger(Reaction.TOXIC_IGNITION, ch);
					if (Dungeon.level.heroFOV[cell]) CellEmitter.center(cell).burst(BlastParticle.FACTORY, 20);
					Sample.INSTANCE.play(Assets.Sounds.BLAST);
					ArrayList<Char> hit = new ArrayList<>();
					for (int i : PathFinder.NEIGHBOURS9){
						Char other = Actor.findChar(cell + i);
						if (other != null) hit.add(other);
						if (gas.cur[cell + i] > 0) gas.clear(cell + i);
					}
					for (Char other : hit){
						other.damage(dmg, new ReactionDamage());
					}
				}

			} else if (buff instanceof Corrosion){
				Bleeding bleed = ch.buff(Bleeding.class);
				if (bleed != null){
					int dmg = Math.max(1, Math.round(bleed.level()));
					bleed.detach();
					ch.damage(dmg, new ReactionDamage());
					trigger(Reaction.CAUSTIC_BLOOD, ch);
				}
			}
		} finally {
			reacting = false;
		}
	}
}
