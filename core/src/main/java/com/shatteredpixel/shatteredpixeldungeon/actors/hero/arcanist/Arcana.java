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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arcanist;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.ArcaneCodex;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindofMisc;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

//central place for the hooks that arcanist mechanics and talents use in core game logic
public class Arcana {

	private static boolean isArcanist( Hero hero ){
		return hero.heroClass == HeroClass.ARCANIST;
	}

	public static float disenchantFactor( Hero hero ){
		float factor = 1f + 0.2f*hero.pointsInTalent(Talent.DEEP_DISENCHANT);
		if (Evolution.is(hero, HeroSubClass.RELIC_KEEPER)) factor += 0.25f;
		return factor;
	}

	public static boolean inscribeRare( Hero hero ){
		return Evolution.is(hero, HeroSubClass.RUNESMITH)
				&& Random.Float() < 0.34f*hero.pointsInTalent(Talent.RUNE_FUSION);
	}

	public static void onCodexUsed( Hero hero ){
		wardingSigil(hero);
	}

	//called from Talent.onArtifactUsed
	public static void onArtifactUsed( Hero hero ){
		wardingSigil(hero);
		arcaneStrike(hero);
		if (hero.subClass == HeroSubClass.RELIC_KEEPER){
			if (hero.hasTalent(Talent.RELIC_SHIELD)){
				int shield = 2*hero.pointsInTalent(Talent.RELIC_SHIELD);
				Buff.affect(hero, Barrier.class).incShield(shield);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			}
			if (hero.hasTalent(Talent.RELIC_SURGE)){
				for (Item i : hero.belongings){
					if (i instanceof Wand) ((Wand) i).gainCharge(0.1f*hero.pointsInTalent(Talent.RELIC_SURGE));
				}
				Item.updateQuickslot();
			}
		}
	}

	//called from Evolution.onWandUsed hook in Wand
	public static void onWandUsed( Hero hero ){
		wardingSigil(hero);
		arcaneStrike(hero);
	}

	private static void wardingSigil( Hero hero ){
		if (hero.hasTalent(Talent.WARDING_SIGIL) && hero.buff(WardingSigilCooldown.class) == null){
			int shield = 1 + 2*hero.pointsInTalent(Talent.WARDING_SIGIL);
			Buff.affect(hero, Barrier.class).incShield(shield);
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shield), FloatingText.SHIELDING);
			Buff.affect(hero, WardingSigilCooldown.class, 10f);
		}
	}

	private static void arcaneStrike( Hero hero ){
		if (hero.hasTalent(Talent.ARCANE_STRIKE)){
			Buff.prolong(hero, ArcaneStrikeTracker.class, 5f);
		}
	}

	//called from Talent.onAttackProc
	public static int onHeroAttack( Hero hero, Char enemy, int dmg ){
		ArcaneStrikeTracker strike = hero.buff(ArcaneStrikeTracker.class);
		if (strike != null && !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
			// +2/+3 damage
			dmg += 1 + hero.pointsInTalent(Talent.ARCANE_STRIKE);
			strike.detach();
		}
		if (Evolution.is(hero, HeroSubClass.RUNESMITH) && hero.belongings.weapon() != null
				&& hero.belongings.weapon() instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon
				&& ((com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon) hero.belongings.weapon()).enchantment != null){
			dmg = Math.round(dmg * (1f + 0.05f*hero.pointsInTalent(Talent.RUNE_ECHO)));
		}
		return dmg;
	}

	public static void onFoodEaten( Hero hero ){
		if (hero.hasTalent(Talent.ARCANE_MEAL)){
			if (isArcanist(hero)) {
				ArcaneCodex.giveEssence(hero, hero.pointsInTalent(Talent.ARCANE_MEAL));
			} else {
				Buff.prolong(hero, Recharging.class, 1 + hero.pointsInTalent(Talent.ARCANE_MEAL));
			}
			ScrollOfRecharging.charge(hero);
		}
		if (hero.hasTalent(Talent.MANA_MEAL)){
			Buff.prolong(hero, Recharging.class, 1 + 2*hero.pointsInTalent(Talent.MANA_MEAL));
			ScrollOfRecharging.charge(hero);
		}
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.LIQUID_ARCANA)){
			if (isArcanist(hero)) {
				ArcaneCodex.giveEssence(hero, Math.round(factor * hero.pointsInTalent(Talent.LIQUID_ARCANA)));
			} else {
				ArtifactRecharge.chargeArtifacts(hero, factor * (1 + hero.pointsInTalent(Talent.LIQUID_ARCANA)));
			}
		}
	}

	public static void onScrollUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.SCROLL_SIPHON)){
			if (isArcanist(hero)) {
				ArcaneCodex.giveEssence(hero, Math.round(factor * hero.pointsInTalent(Talent.SCROLL_SIPHON)));
			} else {
				Buff.prolong(hero, Recharging.class, factor * (1 + hero.pointsInTalent(Talent.SCROLL_SIPHON)));
			}
		}
	}

	//called from Talent.onItemCollected
	public static void onItemCollected( Hero hero, Item item ){
		if (hero.hasTalent(Talent.ESSENCE_SENSE)
				&& (item instanceof Wand || item instanceof Ring || item instanceof Artifact)){
			//only the first time an item is found, unequipping puts items back into the bag
			if (isArcanist(hero) && !item.essenceSensed) {
				ArcaneCodex.giveEssence(hero, hero.pointsInTalent(Talent.ESSENCE_SENSE));
			}
			item.essenceSensed = true;
			item.cursedKnown = true;
		}
	}

	//accuracy and evasion bonus from resonance and the archivist evolution
	public static float statFactor( Hero hero ){
		float factor = 1f;
		if (hero.hasTalent(Talent.RESONANCE)
				&& hero.belongings.artifact() instanceof KindofMisc
				&& hero.belongings.misc() instanceof KindofMisc
				&& hero.belongings.ring() instanceof KindofMisc){
			factor += 0.05f*hero.pointsInTalent(Talent.RESONANCE);
		}
		if (Evolution.is(hero, HeroSubClass.RELIC_KEEPER)){
			factor += 0.05f*hero.pointsInTalent(Talent.CURATORS_EYE);
		}
		return factor;
	}

	public static int bonusArmor( Hero hero ){
		int armor = 0;
		if (Evolution.is(hero, HeroSubClass.RUNESMITH) && hero.belongings.armor() != null
				&& hero.belongings.armor().glyph != null){
			armor += Random.NormalIntRange(0, hero.pointsInTalent(Talent.RUNE_SHIELD));
		}
		if (Evolution.is(hero, HeroSubClass.RELIC_KEEPER) && hero.hasTalent(Talent.RELIC_BOND)){
			int artifacts = 0;
			if (hero.belongings.artifact() instanceof Artifact) artifacts++;
			if (hero.belongings.misc() instanceof Artifact) artifacts++;
			armor += Random.NormalIntRange(0, artifacts * hero.pointsInTalent(Talent.RELIC_BOND));
		}
		return armor;
	}

	//multiplies artifact recharge speed, see RingOfEnergy.artifactChargeMultiplier
	public static float artifactChargeFactor( Char target ){
		if (!(target instanceof Hero)) return 1f;
		Hero hero = (Hero) target;
		float factor = 1f;
		if (hero.subClass == HeroSubClass.RELIC_KEEPER){
			factor += 0.3f + 0.1f*hero.pointsInTalent(Talent.RELIC_ATTUNEMENT);
		}
		if (Evolution.is(hero, HeroSubClass.RELIC_KEEPER)){
			factor += 0.25f + 0.1f*hero.pointsInTalent(Talent.TIMELESS_RELICS);
		}
		return factor;
	}

	//multiplies enchantment proc chance, see Weapon.Enchantment.genericProcChanceMultiplier
	public static float enchantFactor( Char attacker ){
		if (!(attacker instanceof Hero)) return 1f;
		Hero hero = (Hero) attacker;
		float factor = 1f;
		if (hero.subClass == HeroSubClass.RUNESMITH){
			factor += 0.15f*hero.pointsInTalent(Talent.RUNIC_POWER);
		}
		if (Evolution.is(hero, HeroSubClass.RUNESMITH)){
			factor += 0.25f;
		}
		return factor;
	}

	//multiplies glyph proc chance, see Armor.Glyph.genericProcChanceMultiplier
	public static float glyphFactor( Char defender ){
		if (!(defender instanceof Hero)) return 1f;
		Hero hero = (Hero) defender;
		float factor = 1f;
		if (hero.subClass == HeroSubClass.RUNESMITH){
			factor += 0.15f*hero.pointsInTalent(Talent.RUNIC_WARD);
		}
		if (Evolution.is(hero, HeroSubClass.RUNESMITH)){
			factor += 0.25f;
		}
		return factor;
	}

	public static class WardingSigilCooldown extends FlavourBuff {}

	public static class ArcaneStrikeTracker extends FlavourBuff {
		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(0.4f, 0.6f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
}
