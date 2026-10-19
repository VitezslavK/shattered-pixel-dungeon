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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Endings;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.EyeSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

//awakens when the hero tries to destroy the amulet of yendor. Defeating it ends the run.
public class AmuletGuardian extends Eye {

	{
		HP = HT = 450;
		defenseSkill = 26;
		EXP = 0;
		maxLvl = -2;
		lootChance = 0;
		loot = null;
		spriteClass = GuardianSprite.class;

		state = HUNTING;

		properties.add(Property.BOSS);
		properties.add(Property.INORGANIC);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(18, 28);
	}

	@Override
	public int attackSkill( Char target ) {
		return 36;
	}

	@Override
	protected boolean act() {
		if (!BossHealthBar.isAssigned()){
			BossHealthBar.assignBoss(this);
			yell(Messages.get(this, "notice"));
		}
		Dungeon.level.seal();
		return super.act();
	}

	@Override
	public void damage(int dmg, Object src) {
		boolean bleeding = HP*2 <= HT;
		super.damage(dmg, src);
		if (isAlive() && !bleeding && HP*2 <= HT){
			BossHealthBar.bleed(true);
			sprite.showStatus(CharSprite.WARNING, Messages.get(this, "enraged"));
		}
	}

	@Override
	public void die( Object cause ) {
		super.die( cause );
		Dungeon.level.unseal();
		GameScene.bossSlain();
		yell(Messages.get(this, "defeated"));

		//finish the run once the current turn has fully resolved
		Actor.add(new Actor(){
			{
				actPriority = VFX_PRIO;
			}

			@Override
			protected boolean act() {
				Actor.remove(this);
				Endings.finish(Endings.Ending.DESTROYER);
				return false;
			}
		});
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		BossHealthBar.assignBoss(this);
		if (HP*2 <= HT) BossHealthBar.bleed(true);
	}

	public static class GuardianSprite extends EyeSprite {
		public GuardianSprite(){
			super();
			//drawn on the Eye's own layout (16x18 frames, 256x32 sheet) so its charging animation lines up
			texture( "sprites/mod/amulet_guardian.png" );
			scale.set(1.5f);
		}
	}
}
