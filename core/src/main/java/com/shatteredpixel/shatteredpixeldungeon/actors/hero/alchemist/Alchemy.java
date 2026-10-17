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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.FieldCauldron;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//central place for the hooks that alchemist mechanics and talents use in core game logic
public class Alchemy {

	private static boolean isAlchemist( Hero hero ){
		return hero.heroClass == HeroClass.ALCHEMIST;
	}

	private static void gainEnergy( Hero hero, int amount ){
		if (amount <= 0) return;
		Dungeon.energy += amount;
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.ENERGY);
	}

	public static int transmuteCost( Hero hero ){
		return Math.max(1, FieldCauldron.BASE_TRANSMUTE_COST - hero.pointsInTalent(Talent.ESSENCE_SHIFT));
	}

	//called from Potion.onThrow, after the potion shatters
	public static void onPotionShatter( Potion potion, int cell ){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		int dmg = 0;
		if (isAlchemist(hero)){
			dmg += 2 + hero.lvl/3;
		}
		if (hero.hasTalent(Talent.SPLASH_DAMAGE)){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.SPLASH_DAMAGE), 1 + hero.pointsInTalent(Talent.SPLASH_DAMAGE));
		}
		if (dmg <= 0) return;

		ArrayList<Char> targets = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS9){
			Char ch = Actor.findChar(cell + i);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY){
				targets.add(ch);
			}
		}
		for (Char ch : targets){
			ch.damage(dmg, potion);
		}
	}

	//called from Bomb.explode for every character hit
	public static int bombDamage( Bomb bomb, Char ch, int dmg ){
		Hero hero = Dungeon.hero;
		if (hero == null) return dmg;
		float damage = dmg;
		if (ch.alignment == Char.Alignment.ENEMY){
			damage *= 1f + 0.1f*hero.pointsInTalent(Talent.VOLATILE_MIX);
			if (hero.subClass == HeroSubClass.BOMBARDIER){
				damage *= 1.25f + 0.15f*hero.pointsInTalent(Talent.BIG_BANG);
				if (Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS)){
					damage *= 1f + 0.2f*hero.pointsInTalent(Talent.SIEGE_CHARGE);
				}
			}
			if (Evolution.is(hero, HeroSubClass.BOMBARDIER)){
				damage *= 1.25f;
			}
		} else if (ch == hero && hero.subClass == HeroSubClass.BOMBARDIER){
			damage *= 0.5f;
		}
		return Math.round(damage);
	}

	//called from Bomb.explode after damage is dealt
	public static void afterBombExplode( Bomb bomb, int cell, ArrayList<Char> hit ){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		if (hero.subClass == HeroSubClass.BOMBARDIER && hero.hasTalent(Talent.SHRAPNEL)){
			for (Char ch : hit){
				if (ch.isAlive() && ch.alignment == Char.Alignment.ENEMY){
					Buff.prolong(ch, Cripple.class, 3*hero.pointsInTalent(Talent.SHRAPNEL));
				}
			}
		}
		if (Evolution.is(hero, HeroSubClass.BOMBARDIER)){
			if (Random.Float() < 0.34f*hero.pointsInTalent(Talent.NAPALM)){
				for (int i : PathFinder.NEIGHBOURS9){
					if (!Dungeon.level.solid[cell + i] && !Dungeon.level.water[cell + i]){
						com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(
								com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob.seed(cell + i, 4, Fire.class));
					}
				}
			}
			if (hero.hasTalent(Talent.BLAST_SHIELD)){
				Buff.affect(hero, Barrier.class).incShield(2*hero.pointsInTalent(Talent.BLAST_SHIELD));
			}
			if (Random.Float() < 0.15f*hero.pointsInTalent(Talent.CHAIN_REACTION)){
				if (!new Bomb().collect()){
					Dungeon.level.drop(new Bomb(), hero.pos).sprite.drop();
				}
				GLog.p(Messages.get(Alchemy.class, "chain_reaction"));
			}
		}
	}

	public static int bombRangeBonus(){
		return Evolution.is(Dungeon.hero, HeroSubClass.BOMBARDIER) ? 1 : 0;
	}

	//called from Item.castDelay for potions and bombs
	public static float throwDelayFactor( Char user, Item item ){
		if (user instanceof Hero && (item instanceof Potion || item instanceof Bomb)){
			return 1f - 0.33f*((Hero) user).pointsInTalent(Talent.QUICK_THROW);
		}
		return 1f;
	}

	//called from Hero.damage
	public static int modifyDamageTaken( Hero hero, int dmg, Object src ){
		if (hero.hasTalent(Talent.HAZMAT) && (src instanceof Burning || src instanceof Fire
				|| src instanceof ToxicGas || src instanceof CorrosiveGas || src instanceof Corrosion || src instanceof Ooze)){
			return Math.round(dmg * (1f - 0.25f*hero.pointsInTalent(Talent.HAZMAT)));
		}
		return dmg;
	}

	public static int onHeroAttack( Hero hero, Char enemy, int dmg ){
		ElixirFury fury = hero.buff(ElixirFury.class);
		if (fury != null){
			dmg = Math.round(dmg * (1.25f + 0.05f*hero.pointsInTalent(Talent.STRONG_BREW)));
		}
		return dmg;
	}

	public static void onKill( Hero hero, Mob mob ){
		if (hero.subClass == HeroSubClass.TRANSMUTER
				&& Random.Float() < 0.1f*hero.pointsInTalent(Talent.GOLDEN_TOUCH)){
			Dungeon.level.drop(new Gold().random(), mob.pos).sprite.drop();
		}
		if (Evolution.is(hero, HeroSubClass.TRANSMUTER)
				&& Random.Float() < 0.1f*hero.pointsInTalent(Talent.GOLDEN_CAULDRON)){
			gainEnergy(hero, 1);
		}
	}

	public static void onTransmute( Item result ){
		Hero hero = Dungeon.hero;
		if (Evolution.is(hero, HeroSubClass.TRANSMUTER)
				&& result.isUpgradable()
				&& Random.Float() < 0.2f*hero.pointsInTalent(Talent.PHILOSOPHERS_STONE)){
			result.upgrade();
			GLog.p(Messages.get(Alchemy.class, "philosophers_stone"));
		}
	}

	public static int bonusArmor( Hero hero ){
		if (hero.subClass == HeroSubClass.TRANSMUTER){
			return Random.NormalIntRange(0, hero.pointsInTalent(Talent.MIDAS_SKIN));
		}
		return 0;
	}

	public static void onFoodEaten( Hero hero ){
		if (hero.hasTalent(Talent.TINKERS_MEAL)){
			gainEnergy(hero, hero.pointsInTalent(Talent.TINKERS_MEAL));
		}
	}

	//called from Potion.drink (via Talent.onPotionUsed) and for unknown potions
	public static void onPotionDrunk( Hero hero, Potion potion, boolean wasKnown ){
		if (!wasKnown && hero.hasTalent(Talent.TASTE_TEST)){
			int shield = 1 + 2*hero.pointsInTalent(Talent.TASTE_TEST);
			Buff.affect(hero, Barrier.class).incShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.LIQUID_COURAGE)){
			Buff.prolong(hero, Bless.class, factor * (1 + hero.pointsInTalent(Talent.LIQUID_COURAGE)));
		}
		if (hero.hasTalent(Talent.PANACEA)){
			int toRemove = hero.pointsInTalent(Talent.PANACEA) >= 3 ? Integer.MAX_VALUE : hero.pointsInTalent(Talent.PANACEA);
			for (Buff b : hero.buffs()){
				if (toRemove <= 0) break;
				if (b.type == Buff.buffType.NEGATIVE
						&& !(b instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory)){
					b.detach();
					toRemove--;
				}
			}
		}
		if (Evolution.is(hero, HeroSubClass.TRANSMUTER) && hero.hasTalent(Talent.ELIXIR_OF_LIFE)){
			int heal = Math.min(Math.round(hero.HT * 0.03f * hero.pointsInTalent(Talent.ELIXIR_OF_LIFE)), hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
	}

	public static void onScrollUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.INK_DISTILLATION)
				&& Random.Float() < 0.5f*hero.pointsInTalent(Talent.INK_DISTILLATION)*factor){
			gainEnergy(hero, 1);
		}
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (hero.hasTalent(Talent.FORAGED_REAGENTS)
				&& (item instanceof Plant.Seed || item instanceof Runestone)
				&& Random.Float() < 0.5f*hero.pointsInTalent(Talent.FORAGED_REAGENTS)){
			gainEnergy(hero, 1);
		}
	}

	//damage boost from the elixir of fury ability
	public static class ElixirFury extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.FURY; }
		public void tintIcon(Image icon) { icon.hardlight(1f, 0.5f, 0f); }
		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	//the field cauldron slowly generates alchemical energy
	public static class CauldronEnergy extends Buff {

		{
			revivePersists = true;
		}

		private float progress = 0;

		@Override
		public boolean act() {
			Hero hero = (Hero) target;
			if (hero.belongings.getItem(FieldCauldron.class) == null){
				detach();
				return true;
			}
			if (Regeneration.regenOn()){
				float rate = 1f / 50f;
				rate *= 1f + 0.25f*hero.pointsInTalent(Talent.CAULDRON_MASTERY);
				if (Evolution.is(hero, HeroSubClass.TRANSMUTER)) rate *= 2f;
				progress += rate;
				if (progress >= 1f){
					progress -= 1f;
					Dungeon.energy++;
				}
			}
			spend(TICK);
			return true;
		}

		private static final String PROGRESS = "progress";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(PROGRESS, progress);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			progress = bundle.getFloat(PROGRESS);
		}
	}
}
