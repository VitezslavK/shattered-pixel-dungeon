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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.alchemist;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//turns an enemy into a harmless sheep
public class Polymorph extends ArmorAbility {

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;

		Char ch = Actor.findChar(target);
		if (!canPolymorph(ch)){
			GLog.w(Messages.get(this, "no_target"));
			return;
		}

		ArrayList<Mob> victims = new ArrayList<>();
		victims.add((Mob) ch);
		// one extra adjacent target at +2 and +4
		int extra = hero.pointsInTalent(Talent.FLOCK)/2;
		for (int i : PathFinder.NEIGHBOURS8){
			if (extra <= 0) break;
			Char other = Actor.findChar(ch.pos + i);
			if (canPolymorph(other)){
				victims.add((Mob) other);
				extra--;
			}
		}

		for (Mob m : victims){
			int pos = m.pos;
			if (Random.Float() < 0.25f*hero.pointsInTalent(Talent.SHEEPISH_LUCK)){
				m.rollToDropLoot();
			}
			m.destroy();
			m.sprite.killAndErase();
			Dungeon.level.mobs.remove(m);

			Sheep sheep = new Sheep();
			sheep.initialize(15);
			sheep.pos = pos;
			GameScene.add(sheep);
			CellEmitter.get(pos).burst(Speck.factory(Speck.WOOL), 6);
		}

		if (hero.hasTalent(Talent.WOOL_GATHERING)){
			int heal = Math.min(3*hero.pointsInTalent(Talent.WOOL_GATHERING)*victims.size(), hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		Sample.INSTANCE.play(Assets.Sounds.PUFF);
		hero.sprite.zap(target);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	private static boolean canPolymorph( Char ch ){
		return ch instanceof Mob
				&& ch.alignment == Char.Alignment.ENEMY
				&& Dungeon.level.heroFOV[ch.pos]
				&& !Char.hasProp(ch, Char.Property.BOSS)
				&& !Char.hasProp(ch, Char.Property.MINIBOSS)
				&& !Char.hasProp(ch, Char.Property.IMMOVABLE);
	}

	@Override
	public int icon() {
		return HeroIcon.POLYMORPH;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.FLOCK, Talent.SHEEPISH_LUCK, Talent.WOOL_GATHERING, Talent.HEROIC_ENERGY};
	}
}
