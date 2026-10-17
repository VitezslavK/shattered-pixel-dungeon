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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SoulLantern;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BoneSickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//central place for the hooks that necromancer mechanics and talents use in core game logic
public class Necromancy {

	//called whenever an enemy dies while the hero is alive, regardless of who killed it
	public static void onEnemyDeath( Mob mob ){
		Hero hero = Dungeon.hero;
		if (mob.EXP <= 0) return;

		if (hero.heroClass == HeroClass.NECROMANCER){
			// 0.5 souls per kill, +0.25/+0.5 with soul collector
			SoulLantern.giveSouls(hero, 0.5f + 0.25f*hero.pointsInTalent(Talent.SOUL_COLLECTOR));
		} else if (hero.hasTalent(Talent.SOUL_COLLECTOR)){
			// 0.5/1 turns of artifact recharging
			ArtifactRecharge.chargeArtifacts(hero, 0.5f*hero.pointsInTalent(Talent.SOUL_COLLECTOR));
		}
	}

	//called when the hero directly kills an enemy
	public static void onEnemyKilledByHero( Mob mob ){
		Hero hero = Dungeon.hero;

		if (hero.belongings.attackingWeapon() instanceof BoneSickle){
			SoulLantern.giveSouls(hero, 0.25f);
		}

		if (hero.hasTalent(Talent.BONE_ARMOR)){
			// 1/2 shielding per kill, up to 4/6
			int max = 2 + 2*hero.pointsInTalent(Talent.BONE_ARMOR);
			Barrier barrier = Buff.affect(hero, Barrier.class);
			if (barrier.shielding() < max){
				barrier.incShield(Math.min(hero.pointsInTalent(Talent.BONE_ARMOR), max - barrier.shielding()));
			} else {
				barrier.incShield(0);
			}
		}

		if (hero.subClass == HeroSubClass.SOUL_REAPER){
			SoulHarvest.gain(hero, 1);
			if (hero.hasTalent(Talent.REAPING_STRIKE)
					&& !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
				// 3/6/9% of max HP
				int heal = Math.min(Math.round(hero.HT * 0.03f * hero.pointsInTalent(Talent.REAPING_STRIKE)), hero.HT - hero.HP);
				if (heal > 0){
					hero.HP += heal;
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
			}
		}
	}

	//called from Talent.onAttackProc
	public static int onHeroAttack( Hero hero, Char enemy, int dmg ){

		if (hero.hasTalent(Talent.NECROTIC_STRIKE)){
			boolean allyAdjacent = false;
			for (int i : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(enemy.pos + i);
				if (ch != null && ch != hero && ch.alignment == Char.Alignment.ALLY){
					allyAdjacent = true;
					break;
				}
			}
			if (allyAdjacent){
				// +1-2 / +2-3 damage
				dmg += Random.IntRange(hero.pointsInTalent(Talent.NECROTIC_STRIKE), 1 + hero.pointsInTalent(Talent.NECROTIC_STRIKE));
			}
		}

		SoulHarvest harvest = hero.buff(SoulHarvest.class);
		if (harvest != null && !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
			dmg = Math.round(dmg * harvest.damageFactor());
			int heal = Math.min(Math.round(dmg * harvest.lifestealFactor()), hero.HT - hero.HP);
			if (heal > 0){
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}

		return dmg;
	}

	//called from Char.attack, for damage dealt by allies
	public static float allyDamageBonus( Char attacker, float dmg ){
		if (attacker != Dungeon.hero
				&& attacker.alignment == Char.Alignment.ALLY
				&& Dungeon.hero != null
				&& Dungeon.hero.hasTalent(Talent.BONE_SHARDS)){
			dmg += Dungeon.hero.pointsInTalent(Talent.BONE_SHARDS);
		}
		return dmg;
	}

	//called from Char.damage, for damage taken by allies
	public static float allyDamageTakenFactor( Char defender ){
		if (defender != Dungeon.hero
				&& defender.alignment == Char.Alignment.ALLY
				&& Dungeon.hero != null
				&& Dungeon.hero.hasTalent(Talent.EMPOWERED_UNDEATH)){
			return 1f - 0.1f*Dungeon.hero.pointsInTalent(Talent.EMPOWERED_UNDEATH);
		}
		return 1f;
	}

	//called from Hero.damage, transfers some damage to a nearby ally
	public static int soulLink( Hero hero, int dmg, Object src ){
		if (dmg <= 0 || src instanceof Hunger || !hero.hasTalent(Talent.SOUL_LINK)) return dmg;

		Char closest = null;
		for (Char ch : Actor.chars()){
			if (ch != hero && ch.alignment == Char.Alignment.ALLY && ch.isAlive()
					&& Dungeon.level.distance(hero.pos, ch.pos) <= 3
					&& (closest == null || Dungeon.level.distance(hero.pos, ch.pos) < Dungeon.level.distance(hero.pos, closest.pos))){
				closest = ch;
			}
		}
		if (closest == null) return dmg;

		// 10/20/30% of damage
		int transferred = Math.round(dmg * 0.1f * hero.pointsInTalent(Talent.SOUL_LINK));
		if (transferred > 0){
			closest.damage(transferred, new SoulLinkDamage());
			if (closest.sprite != null) closest.sprite.emitter().burst(ShadowParticle.CURSE, 3);
		}
		return dmg - transferred;
	}

	public static class SoulLinkDamage {}

	//called from Hero.drRoll
	public static int bonusArmor( Hero hero ){
		if (hero.subClass == HeroSubClass.SOUL_REAPER && hero.hasTalent(Talent.DEATHS_EMBRACE)){
			int stacks = SoulHarvest.stacks(hero);
			return Random.NormalIntRange(0, (stacks * hero.pointsInTalent(Talent.DEATHS_EMBRACE) + 1)/2);
		}
		return 0;
	}

	public static void onFoodEaten( Hero hero ){
		if (hero.hasTalent(Talent.GRAVE_MEAL)){
			if (hero.heroClass == HeroClass.NECROMANCER){
				// 1/2 souls
				SoulLantern.giveSouls(hero, hero.pointsInTalent(Talent.GRAVE_MEAL));
				ScrollOfRecharging.charge(hero);
			} else {
				// 2/3 turns of artifact recharging
				ArtifactRecharge.chargeArtifacts(hero, 1 + hero.pointsInTalent(Talent.GRAVE_MEAL));
				ScrollOfRecharging.charge(hero);
			}
		}
		if (hero.hasTalent(Talent.HAUNTING_MEAL)){
			// 2/3 turns of terror to adjacent enemies
			for (int i : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar(hero.pos + i);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY){
					Buff.affect(ch, Terror.class, 1 + hero.pointsInTalent(Talent.HAUNTING_MEAL)).object = hero.id();
				}
			}
		}
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.LIQUID_DEATH)){
			// allies heal 20/30% of max HP
			for (Char ch : Actor.chars()){
				if (ch != hero && ch.alignment == Char.Alignment.ALLY && ch.isAlive()){
					int heal = Math.min(Math.round(factor * ch.HT * (0.1f + 0.1f*hero.pointsInTalent(Talent.LIQUID_DEATH))), ch.HT - ch.HP);
					if (heal > 0){
						ch.HP += heal;
						if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
					}
				}
			}
		}
	}

	public static void onScrollUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.INSCRIBED_SOULS)){
			if (hero.heroClass == HeroClass.NECROMANCER){
				// 1/2 souls
				SoulLantern.giveSouls(hero, factor * hero.pointsInTalent(Talent.INSCRIBED_SOULS));
			} else {
				// 2/3 turns of artifact recharging
				ArtifactRecharge.chargeArtifacts(hero, factor * (1 + hero.pointsInTalent(Talent.INSCRIBED_SOULS)));
			}
			ScrollOfRecharging.charge(hero);
		}
	}

}
