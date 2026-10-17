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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SoulLantern;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Subclass evolutions. Every subclass can evolve once using an Essence of Ascension, gaining a
// unique passive effect and access to a 5th tier of talents. All of their hooks live here.
public class Evolution {

	public static boolean is( Hero hero, HeroSubClass subClass ){
		return hero != null && hero.evolved && hero.subClass == subClass;
	}

	private static boolean melee( Hero hero ){
		return !(hero.belongings.attackingWeapon() instanceof MissileWeapon);
	}

	private static boolean inGrass( Char ch ){
		int t = Dungeon.level.map[ch.pos];
		return t == Terrain.GRASS || t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
	}

	private static void heal( Char ch, int amount ){
		amount = Math.min(amount, ch.HT - ch.HP);
		if (amount > 0){
			ch.HP += amount;
			if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
		}
	}

	//called when the hero evolves
	public static void onEvolve( Hero hero ){
		hero.evolved = true;
		Talent.initEvolutionTalents(hero);
		Buff.affect(hero, EvolutionTracker.class);
	}

	//called from Talent.onAttackProc
	public static int onHeroAttack( Hero hero, Char enemy, int dmg ){
		if (!hero.evolved) return dmg;
		float damage = dmg;
		KindOfWeapon wep = hero.belongings.attackingWeapon();

		switch (hero.subClass){
			case BERSERKER:
				if (melee(hero)){
					// +2% damage per 10% missing HP
					int missingTenths = (int)(10 * (1f - hero.HP/(float)hero.HT));
					damage *= 1f + 0.02f*missingTenths;
				}
				break;
			case GLADIATOR:
				if (melee(hero)) {
					FlurryTracker flurry = hero.buff(FlurryTracker.class);
					if (flurry != null && flurry.object == enemy.id()) {
						damage *= 1f + 0.05f * hero.pointsInTalent(Talent.FLURRY) * flurry.stacks;
						flurry.stacks = Math.min(3, flurry.stacks + 1);
						Buff.prolong(hero, FlurryTracker.class, 5f);
					} else {
						flurry = Buff.prolong(hero, FlurryTracker.class, 5f);
						flurry.object = enemy.id();
						flurry.stacks = 1;
					}
					if (hero.hasTalent(Talent.PRECISE_CUTS)) {
						float bleed = damage * 0.1f * hero.pointsInTalent(Talent.PRECISE_CUTS);
						if (bleed >= 1) Buff.affect(enemy, Bleeding.class).set(bleed);
					}
				}
				break;
			case BATTLEMAGE:
				if (wep instanceof MagesStaff){
					damage *= 1.2f;
					if (hero.hasTalent(Talent.ARCANE_EDGE)){
						for (Item i : hero.belongings){
							if (i instanceof Wand){
								((Wand) i).gainCharge(0.1f * hero.pointsInTalent(Talent.ARCANE_EDGE));
							}
						}
						Item.updateQuickslot();
					}
				}
				break;
			case WARLOCK:
				if (enemy.buff(SoulMark.class) != null){
					damage *= 1f + 0.05f*hero.pointsInTalent(Talent.SOUL_REND);
				}
				break;
			case ASSASSIN:
				if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)){
					damage *= 1f + 0.1f*hero.pointsInTalent(Talent.SHADOW_STRIKE);
				}
				break;
			case FREERUNNER:
				if (!melee(hero)){
					damage *= 1f + 0.1f*hero.pointsInTalent(Talent.TAILWIND);
				}
				break;
			case SNIPER:
				if (!melee(hero)){
					if (!Dungeon.level.adjacent(hero.pos, enemy.pos)) damage *= 1.2f;
					if (enemy.HP == enemy.HT) damage *= 1f + 0.1f*hero.pointsInTalent(Talent.HEADHUNTER);
					if (Random.Float() < 0.15f*hero.pointsInTalent(Talent.RICOCHET)){
						for (int i : PathFinder.NEIGHBOURS8){
							Char other = Actor.findChar(enemy.pos + i);
							if (other != null && other != hero && other.alignment == Char.Alignment.ENEMY){
								other.damage(Math.round(damage/2f), hero);
								break;
							}
						}
					}
				}
				break;
			case CHAMPION:
				if (melee(hero)){
					damage *= 1.1f;
					if (hero.hasTalent(Talent.BATTLE_RHYTHM)){
						Buff.affect(hero, MeleeWeapon.Charger.class).gainCharge(0.05f*hero.pointsInTalent(Talent.BATTLE_RHYTHM));
					}
					if (Random.Float() < 0.1f*hero.pointsInTalent(Talent.CRUSHING_BLOWS)){
						Buff.prolong(enemy, Daze.class, 2f);
					}
				}
				break;
			case MONK:
				if (hero.hasTalent(Talent.CHI_STRIKE)){
					damage += Random.IntRange(hero.pointsInTalent(Talent.CHI_STRIKE), 2 + hero.pointsInTalent(Talent.CHI_STRIKE));
				}
				break;
			case PRIEST:
				if (Char.hasProp(enemy, Char.Property.UNDEAD) || Char.hasProp(enemy, Char.Property.DEMONIC)){
					damage += 2*hero.pointsInTalent(Talent.SACRED_FLAME);
				}
				break;
			case PALADIN:
				if (melee(hero)){
					damage *= 1f + 0.05f*hero.pointsInTalent(Talent.RIGHTEOUS_FURY);
				}
				break;
			case SOUL_REAPER:
				if (melee(hero)
						&& !Char.hasProp(enemy, Char.Property.BOSS)
						&& !Char.hasProp(enemy, Char.Property.MINIBOSS)
						&& enemy.HP - damage <= 0.15f*enemy.HT
						&& enemy.HP > damage){
					//execute!
					damage = enemy.HP;
					if (enemy.sprite != null) enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Evolution.class, "executed"));
					CellEmitter.center(enemy.pos).burst(ShadowParticle.UP, 8);
				}
				break;
			default:
				break;
		}

		return Math.round(damage);
	}

	//called when the hero kills an enemy
	public static void onKill( Hero hero, Mob mob ){
		if (!hero.evolved) return;

		switch (hero.subClass){
			case BERSERKER:
				if (melee(hero) && hero.hasTalent(Talent.BLOODTHIRST)){
					heal(hero, Math.round(hero.HT * 0.02f * hero.pointsInTalent(Talent.BLOODTHIRST)));
				}
				break;
			case BATTLEMAGE:
				if (hero.belongings.attackingWeapon() instanceof MagesStaff
						&& Random.Float() < 0.34f*hero.pointsInTalent(Talent.SPELL_SURGE)){
					((MagesStaff) hero.belongings.attackingWeapon()).gainCharge(1f);
				}
				break;
			case WARLOCK:
				if (mob.buff(SoulMark.class) != null && hero.hasTalent(Talent.UNDYING_HUNGER)){
					heal(hero, Math.round(hero.HT * 0.03f * hero.pointsInTalent(Talent.UNDYING_HUNGER)));
				}
				break;
			case ASSASSIN:
				//re-enter stealth after a kill
				Buff.prolong(hero, Invisibility.class, 2f + hero.pointsInTalent(Talent.SILENT_KILLER));
				break;
			case FREERUNNER:
				if (hero.hasTalent(Talent.GALE_STRIKE)){
					Buff.prolong(hero, Haste.class, hero.pointsInTalent(Talent.GALE_STRIKE));
				}
				break;
			case WARDEN:
				if (Random.Float() < 0.34f*hero.pointsInTalent(Talent.WILD_GROWTH)){
					for (int i : PathFinder.NEIGHBOURS9){
						int cell = mob.pos + i;
						int t = Dungeon.level.map[cell];
						if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
								|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
								&& Dungeon.level.plants.get(cell) == null){
							Level.set(cell, Terrain.HIGH_GRASS);
							GameScene.updateMap(cell);
							CellEmitter.get(cell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
						}
					}
					Dungeon.observe();
				}
				break;
			case PALADIN:
				if (hero.hasTalent(Talent.CONSECRATION)){
					float pct = 0.02f * hero.pointsInTalent(Talent.CONSECRATION);
					heal(hero, Math.round(hero.HT * pct));
					for (int i : PathFinder.NEIGHBOURS8){
						Char ally = Actor.findChar(hero.pos + i);
						if (ally != null && ally.alignment == Char.Alignment.ALLY){
							heal(ally, Math.round(ally.HT * pct));
						}
					}
				}
				break;
			case SOUL_REAPER:
				if (hero.hasTalent(Talent.REAPERS_TOLL)){
					SoulLantern.giveSouls(hero, 0.25f*hero.pointsInTalent(Talent.REAPERS_TOLL));
				}
				break;
			default:
				break;
		}
	}

	//called when one of the hero's allies kills an enemy
	public static void onAllyKill( Char ally ){
		Hero hero = Dungeon.hero;
		if (is(hero, HeroSubClass.BONELORD) && hero.hasTalent(Talent.MARROW_FEAST) && ally.isAlive()){
			heal(ally, Math.round(ally.HT * 0.1f * hero.pointsInTalent(Talent.MARROW_FEAST)));
		}
	}

	//called from Hero.damage
	public static int modifyDamageTaken( Hero hero, int dmg, Object src ){
		if (!hero.evolved || dmg <= 0 || src instanceof Hunger) return dmg;
		float damage = dmg;

		switch (hero.subClass){
			case BERSERKER:
				if (hero.HP < hero.HT/2){
					damage *= 1f - 0.05f*hero.pointsInTalent(Talent.UNSTOPPABLE);
				}
				if (hero.hasTalent(Talent.WAR_CRY)
						&& hero.HP - damage < 0.3f*hero.HT
						&& hero.buff(WarCryCooldown.class) == null){
					Buff.affect(hero, WarCryCooldown.class, 50f);
					for (int i : PathFinder.NEIGHBOURS8){
						Char ch = Actor.findChar(hero.pos + i);
						if (ch != null && ch.alignment == Char.Alignment.ENEMY){
							Buff.affect(ch, Terror.class, 1 + hero.pointsInTalent(Talent.WAR_CRY)).object = hero.id();
						}
					}
					hero.sprite.showStatus(CharSprite.WARNING, Messages.get(Evolution.class, "war_cry"));
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				}
				break;
			case WARLOCK:
				//the lich cheats death once per floor
				LichPhylactery phylactery = hero.buff(LichPhylactery.class);
				if (damage >= hero.HP + hero.shielding()
						&& (phylactery == null || phylactery.depth != Dungeon.depth)){
					damage = Math.max(0, hero.HP + hero.shielding() - 1);
					Buff.affect(hero, LichPhylactery.class).depth = Dungeon.depth;
					hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Evolution.class, "undying"));
					hero.sprite.emitter().burst(ShadowParticle.UP, 12);
					Sample.INSTANCE.play(Assets.Sounds.CURSED);
				}
				break;
			case WARDEN:
				if (src instanceof Char && ((Char) src).alignment == Char.Alignment.ENEMY
						&& hero.hasTalent(Talent.THORNWALL) && inGrass(hero)){
					Char attacker = (Char) src;
					attacker.damage(hero.pointsInTalent(Talent.THORNWALL), hero);
					if (attacker.isAlive() && Random.Float() < 0.15f*hero.pointsInTalent(Talent.THORNWALL)){
						Buff.prolong(attacker, Roots.class, 2f);
					}
				}
				break;
			case MONK:
				if (src instanceof Char && Random.Float() < 0.05f*hero.pointsInTalent(Talent.DEFLECTION)){
					damage = 0;
					hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Evolution.class, "deflected"));
				}
				break;
			case PRIEST:
				if (src instanceof Char && Random.Float() < 0.1f*hero.pointsInTalent(Talent.DIVINE_FAVOR)){
					int shield = 2 + hero.pointsInTalent(Talent.DIVINE_FAVOR);
					Buff.affect(hero, Barrier.class).incShield(shield);
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
				}
				break;
			case PALADIN:
				damage *= 1f - 0.04f*hero.pointsInTalent(Talent.HOLY_BULWARK);
				break;
			default:
				break;
		}

		return Math.round(damage);
	}

	public static float accuracyFactor( Hero hero, KindOfWeapon wep ){
		if (!hero.evolved) return 1f;
		boolean missile = wep instanceof MissileWeapon;
		switch (hero.subClass){
			case GLADIATOR:
				return missile ? 1f : 1.15f;
			case SNIPER:
				return missile ? 1f + 0.1f*hero.pointsInTalent(Talent.STEADY_AIM) : 1f;
			case CHAMPION:
				return missile ? 1f : 1.1f;
			default:
				return 1f;
		}
	}

	public static float evasionFactor( Hero hero, Char enemy ){
		if (!hero.evolved) return 1f;
		switch (hero.subClass){
			case GLADIATOR:
				if (hero.hasTalent(Talent.DUELISTS_GUARD)){
					int adjacent = 0;
					for (int i : PathFinder.NEIGHBOURS8){
						Char ch = Actor.findChar(hero.pos + i);
						if (ch != null && ch.alignment == Char.Alignment.ENEMY) adjacent++;
					}
					if (adjacent <= 1) return 1f + 0.1f*hero.pointsInTalent(Talent.DUELISTS_GUARD);
				}
				return 1f;
			case ASSASSIN:
				return hero.invisible > 0 ? 1f + 0.1f*hero.pointsInTalent(Talent.GHOSTLY_FORM) : 1f;
			case FREERUNNER:
				return 1f + 0.05f*hero.pointsInTalent(Talent.WIND_STEP);
			case MONK:
				return 1.1f;
			case SOUL_REAPER:
				return 1f + 0.02f*hero.pointsInTalent(Talent.SHROUD_OF_DEATH)
						* com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer.SoulHarvest.stacks(hero);
			default:
				return 1f;
		}
	}

	public static float speedFactor( Hero hero ){
		return is(hero, HeroSubClass.FREERUNNER) ? 1.1f : 1f;
	}

	public static int bonusArmor( Hero hero ){
		if (!hero.evolved) return 0;
		switch (hero.subClass){
			case WARDEN:
				return inGrass(hero) ? Random.NormalIntRange(0, 2*hero.pointsInTalent(Talent.NATURES_WARD)) : 0;
			case CHAMPION:
				return Random.NormalIntRange(0, hero.pointsInTalent(Talent.IRON_GUARD));
			case MONK:
				return Random.NormalIntRange(0, 2);
			case PALADIN:
				return Random.NormalIntRange(0, 3);
			default:
				return 0;
		}
	}

	//called whenever the hero zaps a wand
	public static void onWandUsed( Hero hero ){
		if (is(hero, HeroSubClass.BATTLEMAGE) && hero.hasTalent(Talent.MANA_SHIELD)){
			int shield = hero.pointsInTalent(Talent.MANA_SHIELD);
			Buff.affect(hero, Barrier.class).incShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
		}
	}

	//used by skeletal servants
	public static float servantHealthFactor(){
		return is(Dungeon.hero, HeroSubClass.BONELORD) ? 1.5f : 1f;
	}

	public static float servantAccuracyFactor(){
		return is(Dungeon.hero, HeroSubClass.BONELORD) ? 1f + 0.1f*Dungeon.hero.pointsInTalent(Talent.ROYAL_DECREE) : 1f;
	}

	public static int servantArmor(){
		return is(Dungeon.hero, HeroSubClass.BONELORD) ? Random.NormalIntRange(0, 2*Dungeon.hero.pointsInTalent(Talent.BONE_PHALANX)) : 0;
	}

	public static int bonusSoulHarvestStacks( Hero hero ){
		return is(hero, HeroSubClass.SOUL_REAPER) ? hero.pointsInTalent(Talent.GRIM_HARVEST) : 0;
	}

	public static float tomeRechargeFactor( Hero hero ){
		return is(hero, HeroSubClass.PRIEST) ? 1.25f : 1f;
	}

	public static class FlurryTracker extends FlavourBuff {
		public int object;
		public int stacks;

		private static final String OBJECT = "object";
		private static final String STACKS = "stacks";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
			bundle.put(STACKS, stacks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
			stacks = bundle.getInt(STACKS);
		}
	}

	public static class WarCryCooldown extends FlavourBuff {}

	public static class LichPhylactery extends Buff {
		public int depth = -1;

		{
			revivePersists = true;
		}

		private static final String DEPTH = "depth";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DEPTH, depth);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			depth = bundle.getInt(DEPTH);
		}
	}

	//handles per-turn evolution effects
	public static class EvolutionTracker extends Buff {

		{
			revivePersists = true;
		}

		private float regen = 0;
		private float allyRegen = 0;

		@Override
		public boolean act() {
			Hero hero = (Hero) target;
			if (!hero.evolved){
				detach();
				return true;
			}

			switch (hero.subClass){
				case WARLOCK:
					if (hero.hasTalent(Talent.DEATHLY_AURA)){
						for (int i : PathFinder.NEIGHBOURS8){
							Char ch = Actor.findChar(hero.pos + i);
							if (ch != null && ch.alignment == Char.Alignment.ENEMY){
								ch.damage(hero.pointsInTalent(Talent.DEATHLY_AURA), this);
							}
						}
					}
					break;
				case WARDEN:
					if (inGrass(hero)){
						regen += 0.34f;
					}
					break;
				case MONK:
					if (hero.hasTalent(Talent.INNER_PEACE)){
						int[] turns = {10, 7, 5};
						regen += 1f / turns[hero.pointsInTalent(Talent.INNER_PEACE)-1];
					}
					break;
				case PRIEST:
					if (hero.hasTalent(Talent.BLESSED_AURA)){
						allyRegen += 1f / (6 - hero.pointsInTalent(Talent.BLESSED_AURA));
						if (allyRegen >= 1){
							allyRegen--;
							for (Char ch : Actor.chars()){
								if (ch != hero && ch.alignment == Char.Alignment.ALLY
										&& Dungeon.level.distance(hero.pos, ch.pos) <= 3
										&& ch.HP < ch.HT){
									ch.HP++;
								}
							}
						}
					}
					break;
				default:
					break;
			}

			if (regen >= 1){
				regen--;
				if (hero.HP < hero.HT) hero.HP++;
			}

			spend(TICK);
			return true;
		}

		private static final String REGEN = "regen";
		private static final String ALLY_REGEN = "ally_regen";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(REGEN, regen);
			bundle.put(ALLY_REGEN, allyRegen);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			regen = bundle.getFloat(REGEN);
			allyRegen = bundle.getFloat(ALLY_REGEN);
		}
	}

}
