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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.alchemist;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//The alchemist can coat their weapon with a potion. For a number of hits each melee attack
// carries that potion's effect, which turns potions into a fighting resource instead of only
// something to drink or throw.
public class WeaponCoating extends Buff {

	{
		type = buffType.POSITIVE;
	}

	public enum Kind {
		FLAME   (PotionOfLiquidFlame.class,  1f, 0.45f, 0f),
		FROST   (PotionOfFrost.class,        0.4f, 0.8f, 1f),
		TOXIC   (PotionOfToxicGas.class,     0.4f, 1f, 0.3f),
		PARALYTIC(PotionOfParalyticGas.class,1f, 1f, 0.4f),
		HEALING (PotionOfHealing.class,      1f, 0.3f, 0.3f),
		HASTE   (PotionOfHaste.class,        1f, 0.8f, 0.2f),
		SHADOW  (PotionOfInvisibility.class, 0.6f, 0.6f, 0.8f),
		GALE    (PotionOfLevitation.class,   0.7f, 0.9f, 1f),
		INSIGHT (PotionOfMindVision.class,   0.8f, 0.5f, 1f),
		PURITY  (PotionOfPurity.class,       1f, 1f, 1f);

		public final Class<? extends Potion> potion;
		public final float r, g, b;

		Kind( Class<? extends Potion> potion, float r, float g, float b ){
			this.potion = potion;
			this.r = r; this.g = g; this.b = b;
		}

		public String title(){
			return Messages.get(WeaponCoating.class, name().toLowerCase() + "_title");
		}

		public String effect(){
			return Messages.get(WeaponCoating.class, name().toLowerCase() + "_effect");
		}

		public static Kind of( Potion potion ){
			for (Kind k : values()){
				if (k.potion == potion.getClass()) return k;
			}
			return null;
		}
	}

	private Kind kind = Kind.FLAME;
	private int hits = 0;

	public static int coatHits( Hero hero ){
		return 8 + hero.lvl/3;
	}

	public static void coat( Hero hero, Kind kind ){
		WeaponCoating c = Buff.affect(hero, WeaponCoating.class);
		c.kind = kind;
		c.hits = coatHits(hero);
		BuffIndicator.refreshHero();
	}

	public Kind kind(){
		return kind;
	}

	//called on every melee hit, returns the modified damage
	public int onHit( Hero hero, Char enemy, int dmg ){
		switch (kind){
			case FLAME:
				if (enemy.isAlive() && !enemy.isImmune(Burning.class)) Buff.affect(enemy, Burning.class).reignite(enemy, 4f);
				break;
			case FROST:
				if (enemy.isAlive()) Buff.prolong(enemy, Chill.class, 3f);
				break;
			case TOXIC:
				if (enemy.isAlive()) Buff.affect(enemy, Poison.class).extend(3 + Dungeon.scalingDepth()/3f);
				break;
			case PARALYTIC:
				if (enemy.isAlive() && Random.Float() < 0.25f) Buff.prolong(enemy, Paralysis.class, 2f);
				break;
			case HEALING:
				int heal = Math.min(Math.max(1, Math.round(dmg * 0.25f)), hero.HT - hero.HP);
				if (heal > 0){
					hero.HP += heal;
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
				break;
			case HASTE:
				Buff.prolong(hero, Haste.class, 1f);
				break;
			case SHADOW:
				dmg = Math.round(dmg * 1.3f);
				break;
			case GALE:
				if (enemy.isAlive() && !Char.hasProp(enemy, Char.Property.IMMOVABLE)){
					Ballistica trajectory = new Ballistica(enemy.pos, enemy.pos + (enemy.pos - hero.pos), Ballistica.MAGIC_BOLT);
					WandOfBlastWave.throwChar(enemy, trajectory, 2, true, true, this);
				}
				break;
			case INSIGHT:
				dmg += Random.IntRange(1, 2 + Dungeon.scalingDepth()/4);
				break;
			case PURITY:
				for (Buff b : enemy.buffs()){
					if (b.type == buffType.POSITIVE) b.detach();
				}
				break;
		}

		hits--;
		if (hits <= 0){
			detach();
		} else {
			BuffIndicator.refreshHero();
		}
		return dmg;
	}

	@Override
	public int icon() {
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(kind.r, kind.g, kind.b);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(hits);
	}

	@Override
	public String name() {
		return Messages.get(this, "name", kind.title());
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", kind.effect(), hits);
	}

	private static final String KIND = "kind";
	private static final String HITS = "hits";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KIND, kind);
		bundle.put(HITS, hits);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		kind = bundle.getEnum(KIND, Kind.class);
		hits = bundle.getInt(HITS);
	}
}
