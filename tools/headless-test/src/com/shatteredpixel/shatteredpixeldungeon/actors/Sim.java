package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import java.util.ArrayList;

//headless turn simulation: the hero stands still, can't die, and hits whatever is next to it
public class Sim {
	public static java.util.Map<String,Integer> errors = new java.util.TreeMap<>();
	static void err(Actor a, Throwable t){
		StringBuilder sb = new StringBuilder(a.getClass().getName()+": "+t);
		int n=0; for (StackTraceElement e : t.getStackTrace()){ sb.append("\n    at ").append(e); if (++n>=9) break; }
		errors.merge(sb.toString(), 1, Integer::sum);
	}
	static java.lang.reflect.Field fAll, fTime, fNow;
	public static java.util.List<com.watabou.noosa.Group> groups = new ArrayList<>();
	public static java.util.Map<String,Integer> used = new java.util.TreeMap<>();
	static void useRandomItem( Hero hero ){
		ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> items = new ArrayList<>();
		for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : hero.belongings) items.add(i);
		if (items.isEmpty()) return;
		com.shatteredpixel.shatteredpixeldungeon.items.Item it = Random.element(items);
		String name = it.getClass().getName();
		if (name.contains("Amulet") || name.contains("LloydsBeacon") || name.contains("ScrollOfPassage") || name.contains(".bags.")) return;
		ArrayList<String> actions;
		try { actions = it.actions(hero); } catch (Throwable t){ err(hero, t); return; }
		actions.remove("DROP");
		if (!name.contains(".brews.")) actions.remove("THROW");
		if (actions.isEmpty()) return;
		String a = Random.element(actions);
		used.merge(it.getClass().getSimpleName()+":"+a, 1, Integer::sum);
		try {
			hero.curAction = null;
			it.execute(hero, a);
		} catch (Throwable t){
			StringBuilder sb = new StringBuilder("USE "+it.getClass().getName()+" "+a+": "+t);
			int n=0; for (StackTraceElement e : t.getStackTrace()){ sb.append("\n    at ").append(e); if (++n>=9) break; }
			errors.merge(sb.toString(), 1, Integer::sum);
		}
		finishSelections(hero);
		hero.HP = hero.HT;
	}

	public static Object inventory;
	//completes item/cell selections that would normally wait for the player's input
	static void finishSelections( Hero hero ){
		for (int round = 0; round < 3; round++){
			boolean any = false;
			try {
				java.lang.reflect.Field csf = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector"); csf.setAccessible(true);
				com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector cs = (com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector) csf.get(null);
				com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.Listener l = cs.listener;
				if (l != null){
					cs.listener = null; any = true;
					int cell = hero.pos;
					ArrayList<Integer> targets = new ArrayList<>();
					for (Mob m : Dungeon.level.mobs) if (m.alignment == Char.Alignment.ENEMY) targets.add(m.pos);
					if (!targets.isEmpty() && Random.Int(4) != 0) cell = Random.element(targets);
					else cell = Random.Int(Dungeon.level.length());
					used.merge("CELL:"+l.getClass().getName(), 1, Integer::sum);
					try { l.onSelect(cell); } catch (Throwable t){ errSel("CELL "+l.getClass().getName(), t); }
				}
			} catch (Throwable t){ errSel("CELLSETUP", t); }
			try {
				java.lang.reflect.Field sf = com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane.class.getDeclaredField("selector"); sf.setAccessible(true);
				com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector sel = (com.shatteredpixel.shatteredpixeldungeon.windows.WndBag.ItemSelector) sf.get(inventory);
				if (sel != null){
					sf.set(inventory, null); any = true;
					ArrayList<com.shatteredpixel.shatteredpixeldungeon.items.Item> ok = new ArrayList<>();
					for (com.shatteredpixel.shatteredpixeldungeon.items.Item i : hero.belongings){ try { if (sel.itemSelectable(i)) ok.add(i); } catch (Throwable t){ errSel("SELECTABLE "+sel.getClass().getName(), t); break; } }
					com.shatteredpixel.shatteredpixeldungeon.items.Item pick = ok.isEmpty() ? null : Random.element(ok);
					used.merge("ITEM:"+sel.getClass().getName()+(pick==null?"(none)":""), 1, Integer::sum);
					try { sel.onSelect(pick); } catch (Throwable t){ errSel("ITEMSEL "+sel.getClass().getName()+" on "+(pick==null?"null":pick.getClass().getSimpleName()), t); }
				}
			} catch (Throwable t){ errSel("ITEMSETUP", t); }
			//let projectiles and other effects reach their targets
			for (int k = 0; k < 40; k++){
				com.watabou.noosa.Game.elapsed = 0.1f;
				for (com.watabou.noosa.Group g : groups){ try { g.update(); } catch (Throwable t){ } }
			}
			if (!any) break;
		}
	}
	static void errSel(String what, Throwable t){
		StringBuilder sb = new StringBuilder("USE "+what+": "+t);
		int n=0; for (StackTraceElement e : t.getStackTrace()){ sb.append("\n    at ").append(e); if (++n>=9) break; }
		errors.merge(sb.toString(), 1, Integer::sum);
	}
	static java.util.Map<Actor,Integer> stuckCount = new java.util.HashMap<>();
	public static com.watabou.noosa.Group spriteGroup; static int dbg = 0;
	public static void ensureSprites(){
		for (com.shatteredpixel.shatteredpixeldungeon.items.Heap h : Dungeon.level.heaps.valueList()){
			if (h.sprite == null){
				try {
					h.sprite = new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite();
					if (spriteGroup != null) spriteGroup.add(h.sprite);
					h.sprite.link(h);
				} catch (Throwable t){ }
			}
		}
		for (Char ch : Actor.chars()){
			if (ch.sprite != null) continue;
			try {
			if (ch instanceof Mob){
				ch.sprite = ((Mob) ch).sprite();
				if (spriteGroup != null) spriteGroup.add(ch.sprite);
			} else if (ch instanceof Hero){
				ch.sprite = new com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite();
				if (spriteGroup != null) spriteGroup.add(ch.sprite);
			} else continue;
			ch.sprite.link(ch);
			} catch (Throwable t){ }
		}
	}
	static float time(Actor a) throws Exception { return fTime.getFloat(a); }
	@SuppressWarnings("unchecked")
	public static String run( int heroTurns ) throws Exception {
		if (fAll == null){
			fAll = Actor.class.getDeclaredField("all"); fAll.setAccessible(true);
			fTime = Actor.class.getDeclaredField("time"); fTime.setAccessible(true);
			fNow = Actor.class.getDeclaredField("now"); fNow.setAccessible(true);
		}
		java.util.Collection<Actor> all = (java.util.Collection<Actor>) fAll.get(null);
		Hero hero = Dungeon.hero;
		int heroActs = 0, steps = 0, kills = 0;
		while (heroActs < heroTurns && steps < heroTurns*400){
			steps++;
			Actor cur = null; float earliest = Float.MAX_VALUE;
			for (Actor a : all.toArray(new Actor[0])){
				float t = time(a);
				if (t < earliest || t == earliest && (cur == null || a.actPriority > cur.actPriority)){
					earliest = t; cur = a;
				}
			}
			if (cur == null) return "no actors";
			ensureSprites();
			if (hero.isAlive()) { hero.HT = Math.max(hero.HT, 5000); hero.HP = hero.HT; }
			//nothing is "visible", so mobs act instantly instead of waiting for sprite animations
			java.util.Arrays.fill(Dungeon.level.heroFOV, false);
			fNow.setFloat(null, time(cur));
			if (cur == hero){
				heroActs++;
				hero.HT = Math.max(hero.HT, 5000); hero.HP = hero.HT;
				if (heroActs % 40 == 1){
					//pull everything towards the hero, and jump next to a random enemy
					ArrayList<Mob> enemies = new ArrayList<>();
					for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])){
						if (m.alignment == Char.Alignment.ENEMY) { enemies.add(m); m.beckon(hero.pos); }
					}
					if (!enemies.isEmpty()){
						Mob m = Random.element(enemies);
						for (int n : PathFinder.NEIGHBOURS8){
							int c = m.pos + n;
							if (c >= 0 && c < Dungeon.level.length() && Dungeon.level.passable[c] && Actor.findChar(c) == null){
								hero.pos = c;
								Dungeon.level.occupyCell(hero);
								m.beckon(hero.pos);
								break;
							}
						}
					}
				}
				if (heroActs % 5 == 0) useRandomItem(hero);
				try {
				Char target = null;
				for (int n : PathFinder.NEIGHBOURS8){
					Char ch = Actor.findChar(hero.pos + n);
					if (ch != null && ch.alignment == Char.Alignment.ENEMY && !ch.isInvulnerable(Hero.class)) { target = ch; break; }
				}
				if (target != null){
					hero.attack(target);
					if (target.isAlive()) target.damage(Math.max(1, target.HT/6), hero);
					if (!target.isAlive()) kills++;
				}
				} catch (Throwable t){ err(hero, t); }
				hero.spend(1f);
			} else {
				float before = time(cur);
				if (stuckCount.size() > 1000) stuckCount.clear();
				try {
					cur.act();
				} catch (Throwable t){
					err(cur, t);
				}
				//let animations and visual effects play out, their callbacks continue the turn
				for (int k = 0; k < 60 && time(cur) == before && Actor.all().contains(cur); k++){
					com.watabou.noosa.Game.elapsed = 0.1f;
					for (com.watabou.noosa.Group g : groups){
						try { g.update(); } catch (Throwable t){
							//UI widgets aren't set up in the headless test, only report game logic errors
							boolean logic = false;
							for (StackTraceElement e : t.getStackTrace()) if (e.getClassName().contains(".actors.") || e.getClassName().contains(".levels.") || e.getClassName().contains(".items.") || e.getClassName().contains(".mechanics.")) logic = true;
							if (logic) err(cur, t);
						}
					}
				}
				if (time(cur) == before && Actor.all().contains(cur) && stuckCount.merge(cur, 1, Integer::sum) >= 6){
					stuckCount.remove(cur);
					//act() returned without spending time: the real game would be waiting for an animation
					String st = cur.getClass().getName();
					if (cur instanceof Char && ((Char)cur).sprite != null) st += " sprite.visible=" + ((Char)cur).sprite.visible;
					errors.merge("STUCK " + st, 1, Integer::sum);
					if (dbg < 3 && cur instanceof Char && ((Char)cur).sprite != null){
						com.watabou.noosa.MovieClip mc = ((Char)cur).sprite;
						java.lang.reflect.Field ca = com.watabou.noosa.MovieClip.class.getDeclaredField("curAnim"); ca.setAccessible(true);
						java.lang.reflect.Field ft = com.watabou.noosa.MovieClip.class.getDeclaredField("frameTimer"); ft.setAccessible(true);
						java.lang.reflect.Field ls = com.watabou.noosa.MovieClip.class.getDeclaredField("listener"); ls.setAccessible(true);
						Object anim = ca.get(mc);
						System.out.println("DBG2 anim="+anim+" delay="+(anim==null?"":((com.watabou.noosa.MovieClip.Animation)anim).delay)+" looped="+(anim==null?"":((com.watabou.noosa.MovieClip.Animation)anim).looped)+" paused="+mc.paused+" timer="+ft.get(mc)+" listener="+ls.get(mc)+" elapsed="+com.watabou.noosa.Game.elapsed);
					}
					if (dbg++ < 15) System.out.println("DBG stuck "+st+" t="+before+" now="+fNow.getFloat(null)+" heroT="+time(hero)+" state="+(cur instanceof Mob ? ((Mob)cur).state.getClass().getSimpleName() : "")
						+ " groups="+groups.size() + (cur instanceof Char && ((Char)cur).sprite != null ? " parent="+((Char)cur).sprite.parent+" inMobs="+(((Char)cur).sprite.parent==spriteGroup)+" active="+((Char)cur).sprite.active+" exists="+((Char)cur).sprite.exists : ""));
					cur.spend(1f);
				}
			}
			if (!hero.isAlive()){
				StringBuilder b = new StringBuilder();
				for (com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff bf : hero.buffs()) b.append(bf.getClass().getSimpleName()).append(",");
				return "hero died after " + cur.getClass().getName() + " HT=" + hero.HT + " buffs=" + b;
			}
		}
		return "turns="+heroActs+" steps="+steps+" kills="+kills;
	}
}
