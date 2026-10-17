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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer.SkeletalServant;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.necromancer.SoulHarvest;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//the necromancer's class item. Collects souls from slain enemies, which are spent to raise
// skeletal servants or consumed for healing.
public class SoulLantern extends Artifact {

	{
		image = ItemSpriteSheet.ARTIFACT_LANTERN;

		exp = 0;
		levelCap = 10;

		charge = 3;
		partialCharge = 0;
		chargeCap = 5;

		defaultAction = AC_SUMMON;

		unique = true;
		bones = false;
	}

	public static final String AC_SUMMON    = "SUMMON";
	public static final String AC_COMMAND   = "COMMAND";
	public static final String AC_CONSUME   = "CONSUME";

	public static final int BASE_SUMMON_COST = 3;

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (isEquipped( hero ) && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_SUMMON);
			if (!SkeletalServant.getServants().isEmpty()) {
				actions.add(AC_COMMAND);
			}
			actions.add(AC_CONSUME);
		}
		return actions;
	}

	@Override
	public String defaultAction() {
		//once the necromancer has as many servants as they can, the lantern defaults to commanding them
		if (Dungeon.hero != null && SkeletalServant.getServants().size() >= maxServants(Dungeon.hero)){
			return AC_COMMAND;
		}
		return AC_SUMMON;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute(hero, action);

		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals(AC_SUMMON) || action.equals(AC_COMMAND) || action.equals(AC_CONSUME)) {
			if (!isEquipped(hero)) {
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
				return;
			} else if (cursed) {
				GLog.i(Messages.get(this, "cursed"));
				return;
			}
		}

		if (action.equals(AC_SUMMON)) {
			summon(hero);
		} else if (action.equals(AC_COMMAND)) {
			command();
		} else if (action.equals(AC_CONSUME)) {
			consume(hero);
		}
	}

	public static int maxServants( Hero hero ){
		int max = 1;
		if (hero.subClass == HeroSubClass.BONELORD){
			max++;
			if (hero.pointsInTalent(Talent.LEGION) >= 2) max++;
		}
		return max;
	}

	public static int summonCost( Hero hero ){
		int cost = BASE_SUMMON_COST;
		if (hero.subClass == HeroSubClass.BONELORD){
			if (hero.pointsInTalent(Talent.LEGION) >= 1) cost--;
			if (hero.pointsInTalent(Talent.LEGION) >= 3) cost--;
		}
		return cost;
	}

	private void summon( Hero hero ){
		if (SkeletalServant.getServants().size() >= maxServants(hero)){
			GLog.w(Messages.get(this, "too_many"));
			return;
		}
		int cost = summonCost(hero);
		if (charge < cost){
			GLog.w(Messages.get(this, "no_charge"));
			return;
		}

		ArrayList<Integer> cells = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int cell = hero.pos + i;
			if (Dungeon.level.passable[cell] && !Dungeon.level.avoid[cell] && Actor.findChar(cell) == null){
				cells.add(cell);
			}
		}
		if (cells.isEmpty()){
			GLog.w(Messages.get(this, "no_space"));
			return;
		}

		SkeletalServant servant = new SkeletalServant();
		servant.pos = Random.element(cells);
		GameScene.add(servant);
		ScrollOfTeleportation.appear(servant, servant.pos);
		servant.sprite.emitter().burst(ShadowParticle.UP, 8);

		spendCharge(cost);
		Talent.onArtifactUsed(hero);

		Sample.INSTANCE.play(Assets.Sounds.BONES);
		hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(Actor.TICK);
	}

	private void command(){
		if (SkeletalServant.getServants().isEmpty()){
			GLog.w(Messages.get(this, "no_servants"));
			return;
		}
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				for (SkeletalServant servant : SkeletalServant.getServants()){
					servant.directTocell(cell);
				}
			}

			@Override
			public String prompt() {
				return Messages.get(SoulLantern.class, "prompt_command");
			}
		});
	}

	private void consume( Hero hero ){
		if (charge < 1){
			GLog.w(Messages.get(this, "no_charge"));
			return;
		}

		//heals 8% of max HP + lantern level, boosted by soul feast
		float healing = Math.round(hero.HT * 0.08f) + level();
		if (hero.subClass == HeroSubClass.SOUL_REAPER) {
			healing *= 1f + 0.25f*hero.pointsInTalent(Talent.SOUL_FEAST);
			SoulHarvest.gain(hero, 1 + hero.pointsInTalent(Talent.SOUL_FEAST));
		}
		int heal = Math.min(Math.round(healing), hero.HT - hero.HP);
		hero.HP += heal;
		if (heal > 0) {
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		hero.sprite.emitter().burst(ShadowParticle.UP, 6);

		spendCharge(1);
		Talent.onArtifactUsed(hero);

		Sample.INSTANCE.play(Assets.Sounds.DRINK);
		hero.sprite.operate(hero.pos);
		hero.spendAndNext(Actor.TICK);
	}

	public void spendCharge( int chargesSpent ){
		charge -= chargesSpent;

		//target hero level is 1 + 2*lantern level, same as the holy tome
		int lvlDiffFromTarget = Dungeon.hero.lvl - (1+level()*2);
		if (level() >= 7){
			lvlDiffFromTarget -= level()-6;
		}

		if (lvlDiffFromTarget >= 0){
			exp += Math.round(chargesSpent * 10f * Math.pow(1.1f, lvlDiffFromTarget));
		} else {
			exp += Math.round(chargesSpent * 10f * Math.pow(0.75f, -lvlDiffFromTarget));
		}

		if (exp >= (level() + 1) * 40 && level() < levelCap) {
			upgrade();
			Catalog.countUse(SoulLantern.class);
			exp -= level() * 40;
			GLog.p(Messages.get(this, "levelup"));
		}

		updateQuickslot();
	}

	//adds souls directly, used when enemies die and by talents
	public void gainSouls( float amount ){
		if (cursed || Dungeon.hero == null || Dungeon.hero.buff(MagicImmune.class) != null) return;
		if (charge < chargeCap) {
			partialCharge += amount;
			while (partialCharge >= 1f) {
				charge++;
				partialCharge--;
			}
			if (charge >= chargeCap){
				partialCharge = 0;
				charge = chargeCap;
			}
			updateQuickslot();
		}
	}

	//gives souls to the hero's equipped lantern, if they have one
	public static void giveSouls( Hero hero, float amount ){
		SoulLantern lantern = hero.belongings.getItem(SoulLantern.class);
		if (lantern != null && lantern.isEquipped(hero)){
			lantern.gainSouls(amount);
		}
	}

	@Override
	public Item upgrade() {
		super.upgrade();
		chargeCap = 5 + level()/2;
		return this;
	}

	@Override
	public void level(int value) {
		super.level(value);
		chargeCap = 5 + level()/2;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new LanternRecharge();
	}

	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;
		gainSouls(amount/15f);
	}

	@Override
	public String desc() {
		String desc = super.desc();
		if (isEquipped(Dungeon.hero) && !cursed){
			desc += "\n\n" + Messages.get(this, "desc_stats",
					summonCost(Dungeon.hero),
					SkeletalServant.getServants().size(),
					maxServants(Dungeon.hero));
		}
		if (cursed && isEquipped(Dungeon.hero)){
			desc += "\n\n" + Messages.get(this, "desc_cursed");
		}
		return desc;
	}

	public class LanternRecharge extends ArtifactBuff {

		@Override
		public boolean act() {
			//souls very slowly trickle in on their own, roughly 1 per 75 turns
			if (charge < chargeCap && !cursed && target.buff(MagicImmune.class) == null) {
				if (Regeneration.regenOn()) {
					partialCharge += RingOfEnergy.artifactChargeMultiplier(target) / 75f;
				}
				while (partialCharge >= 1) {
					charge++;
					partialCharge -= 1;
					if (charge == chargeCap){
						partialCharge = 0;
					}
				}
			} else {
				partialCharge = Math.min(partialCharge, 0.99f);
			}

			updateQuickslot();
			spend( TICK );
			return true;
		}
	}

}
