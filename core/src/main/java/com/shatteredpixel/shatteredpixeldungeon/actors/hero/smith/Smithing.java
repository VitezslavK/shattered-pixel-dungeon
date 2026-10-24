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
package com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Scrap;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SmithHammer;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//Everything the Smith does differently: weapon heat, scrap, and the anvil's fusing and reinforcing.
// Called from the same places as the other mod classes' hubs (Talent, Hero, Mob, MeleeWeapon).
public class Smithing {

	public static boolean isSmith( Hero hero ){
		return hero != null && hero.heroClass == HeroClass.SMITH;
	}

	public static Heat heat( Hero hero ){
		Heat h = hero.buff(Heat.class);
		return h != null ? h : Buff.affect(hero, Heat.class);
	}

	public static void heal( Char ch, int amount ){
		amount = Math.min(amount, ch.HT - ch.HP);
		if (amount > 0){
			ch.HP += amount;
			if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
		}
	}

	private static boolean melee( Hero hero ){
		return !(hero.belongings.attackingWeapon() instanceof MissileWeapon);
	}

	//------------------------------ heat ------------------------------

	public static int onHeroAttack( Hero hero, Char enemy, int dmg ){
		if (!isSmith(hero) || !melee(hero)) return dmg;
		Heat h = heat(hero);
		boolean flamesmith = hero.subClass == HeroSubClass.FLAMESMITH;

		float damage = dmg;
		float burnChance = 0;
		if (h.overheated()){
			damage *= 1.4f;
			burnChance = 0.5f;
			if (flamesmith){
				damage *= 1f + 0.05f*hero.pointsInTalent(Talent.BLAZING_STRIKES);
				if (hero.hasTalent(Talent.BLAZING_STRIKES)) burnChance = 1f;
			}
		} else if (h.glowing()){
			damage *= 1.2f;
			burnChance = 0.2f;
		}
		if (burnChance > 0 && enemy.isAlive() && !enemy.isImmune(Burning.class) && Random.Float() < burnChance){
			Buff.affect(enemy, Burning.class).reignite(enemy);
		}

		//an overheated weapon burns everyone but the Flamesmith
		if (h.overheated() && !flamesmith){
			int burn = Random.IntRange(1, 2 + Dungeon.depth/5);
			burn = Math.round(burn * (1f - 0.25f*hero.pointsInTalent(Talent.HEAT_RESISTANCE)));
			if (burn > 0) hero.damage(burn, h);
		}

		float before = h.heat();
		float gain = 10f * (1f + 0.25f*hero.pointsInTalent(Talent.HAMMER_BLOWS));
		if (hero.belongings.attackingWeapon() instanceof SmithHammer) gain += 5;
		h.add(gain);

		if (flamesmith && before < Heat.MAX && h.heat() >= Heat.MAX){
			heatBurst(hero, h);
		}
		return Math.round(damage);
	}

	//the Flamesmith releases a full forge's heat in a blast around them
	public static void heatBurst( Hero hero, Heat h ){
		int dmg = Math.round((3 + hero.lvl/2f) * (1f + 0.2f*hero.pointsInTalent(Talent.SUPERNOVA)));
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = hero.pos + i;
			Char ch = Actor.findChar(cell);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY){
				ch.damage(dmg, h);
				if (ch.isAlive() && !ch.isImmune(Burning.class)) Buff.affect(ch, Burning.class).reignite(ch);
			}
			if (Dungeon.level.heroFOV[cell]) CellEmitter.get(cell).burst(FlameParticle.FACTORY, 5);
		}
		if (Evolution.is(hero, HeroSubClass.FLAMESMITH)){
			heal(hero, 3*hero.pointsInTalent(Talent.PHOENIX_HEART));
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		GLog.p(Messages.get(Smithing.class, "heat_burst"));
		h.set(50);
	}

	//dunking the hot weapon in water hardens it, and the Smith with it
	public static void quench( Hero hero, Heat h ){
		float removed = h.heat();
		h.set(0);
		Buff.prolong(hero, Tempered.class, 5f + 5f*hero.pointsInTalent(Talent.TEMPERED_STEEL));
		heal(hero, Math.round(hero.pointsInTalent(Talent.TEMPERED_STEEL) * removed / 20f));
		if (hero.sprite != null){
			CellEmitter.get(hero.pos).burst(Speck.factory(Speck.STEAM), 8);
		}
		Sample.INSTANCE.play(Assets.Sounds.BURNING, 0.6f, 1.6f);
		GLog.i(Messages.get(Smithing.class, "quenched"));
	}

	//------------------------------ defence ------------------------------

	public static int bonusArmor( Hero hero ){
		int armor = 0;
		if (hero.buff(Tempered.class) != null) armor += 1 + hero.lvl/8;
		IronWall wall = hero.buff(IronWall.class);
		if (wall != null) armor += 4 + hero.lvl/3 + hero.pointsInTalent(Talent.WALKING_FORTRESS);
		if (Evolution.is(hero, HeroSubClass.ANVIL_MASTER) && hero.hasTalent(Talent.SCRAP_ARMOR)){
			armor += Math.min(hero.pointsInTalent(Talent.SCRAP_ARMOR), scrap(hero) / 10);
		}
		return armor;
	}

	public static int modifyDamageTaken( Hero hero, int dmg, Object src ){
		if (src instanceof Burning || src instanceof Fire || src instanceof Heat){
			float factor = 1f - 0.25f*hero.pointsInTalent(Talent.HEAT_RESISTANCE)
					- 0.33f*hero.pointsInTalent(Talent.FIRE_WALKER);
			dmg = Math.max(0, Math.round(dmg * Math.max(0, factor)));
		}
		if (src instanceof Char && ((Char) src).isAlive() && Dungeon.level.adjacent(hero.pos, ((Char) src).pos)){
			Char attacker = (Char) src;
			IronWall wall = hero.buff(IronWall.class);
			if (wall != null && dmg > 0){
				int back = Math.round(dmg * (0.2f + 0.1f*hero.pointsInTalent(Talent.THORNED_IRON)));
				if (back > 0) attacker.damage(back, wall);
			}
			if (Evolution.is(hero, HeroSubClass.FLAMESMITH) && heat(hero).overheated()
					&& Random.Float() < 0.33f*hero.pointsInTalent(Talent.MAGMA_SKIN)
					&& attacker.isAlive() && !attacker.isImmune(Burning.class)){
				Buff.affect(attacker, Burning.class).reignite(attacker);
			}
		}
		return dmg;
	}

	//------------------------------ talents on use ------------------------------

	public static void onFoodEaten( Hero hero ){
		if (hero.hasTalent(Talent.HOT_MEAL)){
			heat(hero).add(20f * hero.pointsInTalent(Talent.HOT_MEAL));
		}
		if (hero.hasTalent(Talent.SMOKED_MEAL)){
			Buff.affect(hero, Barrier.class).incShield(1 + 2*hero.pointsInTalent(Talent.SMOKED_MEAL));
		}
	}

	public static void onPotionUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.MOLTEN_DRAUGHT)){
			heat(hero).add(25f * factor * hero.pointsInTalent(Talent.MOLTEN_DRAUGHT));
		}
	}

	public static void onScrollUsed( Hero hero, float factor ){
		if (hero.hasTalent(Talent.INSCRIBED_STEEL)){
			Buff.prolong(hero, Tempered.class, 5f * factor * hero.pointsInTalent(Talent.INSCRIBED_STEEL));
		}
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (!hero.hasTalent(Talent.SMITHS_EYE)) return;
		if (item instanceof Weapon || item instanceof Armor){
			if (hero.pointsInTalent(Talent.SMITHS_EYE) >= 2) item.identify();
			else item.cursedKnown = true;
		}
	}

	//------------------------------ scrap ------------------------------

	public static int scrap( Hero hero ){
		Scrap s = hero.belongings.getItem(Scrap.class);
		return s == null ? 0 : s.quantity();
	}

	public static boolean spendScrap( Hero hero, int amount ){
		Scrap s = hero.belongings.getItem(Scrap.class);
		if (s == null || s.quantity() < amount) return false;
		if (s.quantity() == amount) s.detachAll(hero.belongings.backpack);
		else s.quantity(s.quantity() - amount);
		Item.updateQuickslot();
		return true;
	}

	public static void giveScrap( Hero hero, int amount ){
		if (amount <= 0) return;
		Scrap s = new Scrap();
		s.quantity(amount);
		if (!s.collect()){
			Heap h = Dungeon.level.drop(s, hero.pos);
			if (h.sprite != null) h.sprite.drop();
		}
	}

	public static void onKill( Hero hero, Mob mob ){
		if (!isSmith(hero)) return;
		int amount = 0;
		boolean anvilMaster = hero.subClass == HeroSubClass.ANVIL_MASTER;
		if (Char.hasProp(mob, Char.Property.INORGANIC)){
			amount += Random.IntRange(1, 2);
			if (anvilMaster) amount += hero.pointsInTalent(Talent.SCRAP_MAGNET);
		}
		float chance = 0.1f*hero.pointsInTalent(Talent.SCRAP_COLLECTOR);
		if (anvilMaster) chance += 0.05f*hero.pointsInTalent(Talent.SCRAP_MAGNET);
		if (Random.Float() < chance) amount++;
		if (amount > 0){
			Scrap s = new Scrap();
			s.quantity(amount);
			Heap h = Dungeon.level.drop(s, mob.pos);
			if (h.sprite != null) h.sprite.drop();
		}
	}

	//scrap from melting down a weapon or armor at the anvil
	public static int smeltValue( Hero hero, Item item ){
		int tier = 1;
		if (item instanceof MeleeWeapon)    tier = ((MeleeWeapon) item).tier;
		else if (item instanceof MissileWeapon) tier = ((MissileWeapon) item).tier;
		else if (item instanceof Armor)     tier = ((Armor) item).tier;
		int value = 2 + 2*tier + 4*Math.max(0, item.level());
		if (item instanceof MissileWeapon) value = Math.max(1, value * item.quantity() / 3);
		value += hero.pointsInTalent(Talent.QUICK_FORGE);
		return value;
	}

	//------------------------------ reinforcing ------------------------------

	public static int maxReinforce( Hero hero ){
		int max = 3;
		if (hero.subClass == HeroSubClass.ANVIL_MASTER) max += hero.pointsInTalent(Talent.REFORGE);
		return max;
	}

	public static int reinforceCost( Hero hero, Weapon w ){
		float cost = 8 + 4*w.reinforced;
		cost *= 1f - 0.15f*hero.pointsInTalent(Talent.MASTER_REINFORCE);
		return Math.max(1, Math.round(cost));
	}

	public static int reinforceDamage( MeleeWeapon w ){
		if (w.reinforced <= 0) return 0;
		int bonus = w.reinforced;
		Hero hero = Dungeon.hero;
		if (hero != null && Evolution.is(hero, HeroSubClass.ANVIL_MASTER)){
			bonus += Math.round(w.reinforced * 0.5f * hero.pointsInTalent(Talent.MASTERWORK));
		}
		return bonus;
	}

	//------------------------------ fusing ------------------------------

	public static int maxFusions( Hero hero ){
		if (Evolution.is(hero, HeroSubClass.ANVIL_MASTER)) return 3;
		if (hero.subClass == HeroSubClass.ANVIL_MASTER) return 2;
		return 1;
	}

	public static int fusionCost( MeleeWeapon donor ){
		return 6 + 2*donor.tier;
	}

	public static boolean canBeFused( MeleeWeapon w ){
		return !(w instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff) && !w.unique;
	}

	//the main weapon keeps its type and gains the donor's reach, speed, accuracy,
	// some of its upgrades and its enchantment if it had none
	public static void fuse( Hero hero, MeleeWeapon main, MeleeWeapon donor ){
		boolean master = hero.subClass == HeroSubClass.ANVIL_MASTER;
		int levels = Math.max(0, donor.level());
		if (!master) levels /= 2;
		if (levels > 0) main.level(main.level() + levels);

		float acc = Math.max(main.ACC, donor.ACC) + 0.1f*hero.pointsInTalent(Talent.FUSION_MASTERY);
		if (Evolution.is(hero, HeroSubClass.ANVIL_MASTER)){
			acc += 0.05f*hero.pointsInTalent(Talent.FLAWLESS_FORGE);
		}
		main.applyFusion(Math.max(main.RCH, donor.RCH), Math.min(main.DLY, donor.DLY), acc);

		if (main.enchantment == null && donor.enchantment != null && !donor.hasCurseEnchant()){
			main.enchant(donor.enchantment);
		}
		main.fusions++;
		main.fusedNames = main.fusedNames.isEmpty() ? donor.name() : main.fusedNames + ", " + donor.name();
	}

	public static String weaponInfo( MeleeWeapon w ){
		String info = "";
		if (w.fusions > 0){
			info += Messages.get(Smithing.class, "info_fused", w.fusedNames, w.RCH, Math.round(100/w.DLY), Math.round(100*w.ACC));
		}
		if (w.reinforced > 0){
			if (!info.isEmpty()) info += "\n";
			info += Messages.get(Smithing.class, "info_reinforced", w.reinforced, reinforceDamage(w));
		}
		return info;
	}

	//------------------------------ buffs ------------------------------

	//after quenching, the Smith's skin and gear are hardened for a while
	public static class Tempered extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.5f, 0.8f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (10f - visualcooldown()) / 10f);
		}

		@Override
		public String desc() {
			int armor = target instanceof Hero ? 1 + ((Hero) target).lvl/8 : 1;
			return Messages.get(this, "desc", armor, dispTurns());
		}
	}

	//the Iron Wall armor ability
	public static class IronWall extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.7f, 0.7f, 0.75f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (20f - visualcooldown()) / 20f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
