import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import java.util.ArrayList;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Fortune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.GoldenAltar;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.bombs.Bomb;
import com.shatteredpixel.shatteredpixeldungeon.items.gold.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.smith.*;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.ShopRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;

//checks the Smith class mechanics without a screen
public class SmithTest {
  static int fails = 0;
  static void check(boolean ok, String what){ System.out.println((ok ? "OK   " : "FAIL ") + what); if (!ok) fails++; }
  static void startRun(HeroClass cls, int depth){
    GamesInProgress.selectedClass = cls; GamesInProgress.curSlot = 1;
    Dungeon.daily = false;
    Dungeon.initSeed();
    Dungeon.init();
    Dungeon.depth = depth;
    Random.pushGenerator(Dungeon.seedCurDepth());
    Level l = Dungeon.newLevel();
    Random.popGenerator();
    Dungeon.level = l;
    com.watabou.utils.PathFinder.setMapSize(l.width(), l.height());
    Dungeon.hero.pos = l.entrance();
  }

  static void fakeScene() throws Exception {
    {
      Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true);
      sun.misc.Unsafe u = (sun.misc.Unsafe) uf.get(null);
      Game g = (Game) u.allocateInstance(ShatteredPixelDungeon.class);
      Game.instance = g;
      Field sf = Game.class.getDeclaredField("scene"); sf.setAccessible(true);
      com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene gs = (com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene) u.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class);
      for (Class<?> c = gs.getClass(); c != Object.class; c = c.getSuperclass()){
        for (Field f : c.getDeclaredFields()){
          if (Modifier.isStatic(f.getModifiers())) continue;
          if (com.watabou.noosa.Group.class.isAssignableFrom(f.getType())){
            try { f.setAccessible(true); Constructor<?> k = f.getType().getDeclaredConstructor(); k.setAccessible(true); Object gv = k.newInstance(); f.set(gs, gv); } catch (Throwable t){ }
          }
        }
      }
      try { Field members = com.watabou.noosa.Group.class.getDeclaredField("members"); members.setAccessible(true); members.set(gs, new ArrayList<>()); } catch (Throwable t){ System.out.println("members "+t); }
      { Field cs = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector"); cs.setAccessible(true); cs.set(null, u.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.class)); }
      { try { Field uc = com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.class.getDeclaredField("uiCamera"); uc.setAccessible(true); uc.set(Modifier.isStatic(uc.getModifiers()) ? null : gs, new com.watabou.noosa.Camera(0,0,800,800,2f)); } catch (Throwable t){ System.out.println("uicam "+t); } }
      Gdx.graphics = (Graphics) Proxy.newProxyInstance(SmithTest.class.getClassLoader(), new Class[]{Graphics.class}, (p,m,args)->{
        Class<?> r = m.getReturnType();
        if (r==boolean.class) return false; if (r==int.class) return 800; if (r==long.class) return 0L; if (r==float.class) return 1f; if (r==double.class) return 1d; return null;
      });
      {
        Object inv = u.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane.class);
        Field vis = com.watabou.noosa.Gizmo.class.getDeclaredField("visible"); vis.setAccessible(true); vis.setBoolean(inv, true);
        Field invf = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("inventory"); invf.setAccessible(true); invf.set(gs, inv);
        Field mem = com.watabou.noosa.Group.class.getDeclaredField("members"); mem.setAccessible(true); if (mem.get(gs) == null) mem.set(gs, new ArrayList<>());
      }
      Field gsf = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("scene"); gsf.setAccessible(true); gsf.set(null, gs);
      sf.set(g, gs);
    }
  }

  public static void main(String[] a) throws Exception {
        File prefsDir = new File(a[0]); prefsDir.mkdirs();
    Gdx.files = new Lwjgl3Files();
    Gdx.net = new Lwjgl3Net(new Lwjgl3ApplicationConfiguration());
    Gdx.app = (Application) Proxy.newProxyInstance(SmithTest.class.getClassLoader(), new Class[]{Application.class}, (p,m,args)->{
      switch (m.getName()){
        case "getPreferences": return new Lwjgl3Preferences(new FileHandle(new File(prefsDir,(String)args[0])));
        case "getType": return Application.ApplicationType.Desktop;
        case "postRunnable": ((Runnable)args[0]).run(); return null;
        case "log": case "error": case "debug": System.out.println("  [game] " + args[1]); return null;
        case "hashCode": return 1; case "equals": return false; case "toString": return "app";
      }
      Class<?> r = m.getReturnType(); if (r==boolean.class) return false; if (r==int.class||r==long.class) return 0; return null;
    });
    com.badlogic.gdx.utils.GdxNativesLoader.load();
    Gdx.gl = Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) Proxy.newProxyInstance(SmithTest.class.getClassLoader(), new Class[]{com.badlogic.gdx.graphics.GL20.class, com.badlogic.gdx.graphics.GL30.class}, (p,m,args)->{
      Class<?> r = m.getReturnType(); if (r==boolean.class) return false; if (r==int.class) return 1; if (r==long.class) return 0L; if (r==float.class) return 0f; if (r==String.class) return ""; return null; });
    com.watabou.utils.FileUtils.setDefaultFileProperties(Files.FileType.Absolute, prefsDir.getAbsolutePath()+"/");
    Game.version = "3.3.8"; Game.versionCode = 896;
    Messages.setup(Languages.ENGLISH);


    Dungeon.hero = null;
    startRun(HeroClass.SMITH, 1);
    fakeScene();
    Hero hero = Dungeon.hero;

    // --- starting kit
    check(hero.belongings.weapon instanceof SmithHammer, "starts with the forging hammer");
    check(hero.belongings.getItem(PortableAnvil.class) != null, "starts with the portable anvil");
    check(Smithing.scrap(hero) == 10, "starts with 10 scrap (" + Smithing.scrap(hero) + ")");
    check(hero.buff(Heat.class) != null, "has the heat buff");
    check(hero.talents.get(0).containsKey(Talent.HOT_MEAL) && hero.talents.get(1).containsKey(Talent.QUICK_FORGE)
        && hero.talents.get(2).containsKey(Talent.TEMPERED_STEEL), "smith talents in tiers 1-3");
    check(HeroClass.SMITH.armorAbilities().length == 3, "3 armor abilities");
    check(HeroClass.SMITH.isUnlocked(), "always unlocked");

    // --- heat
    com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat rat = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Rat();
    rat.HP = rat.HT = 1000;
    Heat h = Smithing.heat(hero);
    h.set(0);
    int d0 = Smithing.onHeroAttack(hero, rat, 10);
    check(d0 == 10, "cold weapon: normal damage");
    check(Math.abs(h.heat() - 15) < 0.01f, "a hammer hit adds 15 heat (" + h.heat() + ")");
    h.set(50);
    check(Smithing.onHeroAttack(hero, rat, 10) == 12, "glowing weapon: +20% damage");
    h.set(85);
    int hp = hero.HP;
    int d2 = 0;
    try { d2 = Smithing.onHeroAttack(hero, rat, 10); } catch (Exception e){ check(false, "overheated hit threw " + e); }
    check(d2 == 14, "overheated weapon: +40% damage");
    check(hero.HP < hp, "overheated weapon burns the smith (" + hp + " -> " + hero.HP + ")");
    hero.HP = hero.HT;

    // flamesmith: no self burn, burst at 100
    hero.subClass = HeroSubClass.FLAMESMITH;
    h.set(95); hp = hero.HP;
    Smithing.onHeroAttack(hero, rat, 10);
    check(hero.HP == hp, "flamesmith isn't burned by overheat");
    check(Math.abs(h.heat() - 50) < 0.01f, "reaching 100 heat triggers the burst and drops to 50 (" + h.heat() + ")");
    hero.subClass = HeroSubClass.NONE;

    // quench
    h.set(60);
    Smithing.quench(hero, h);
    check(h.heat() == 0 && hero.buff(Smithing.Tempered.class) != null, "quenching cools fully and tempers");
    check(Smithing.bonusArmor(hero) >= 1, "tempered gives bonus armor");

    // --- fusion
    MeleeWeapon main = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
    MeleeWeapon spear = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Spear();
    MeleeWeapon dagger = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger();
    spear.level(4);
    Smithing.fuse(hero, main, spear);
    check(main.RCH == 2, "fused sword gets the spear's reach (" + main.RCH + ")");
    check(main.level() == 2, "fusion transfers half the levels (" + main.level() + ")");
    check(main.fusions == 1 && Smithing.maxFusions(hero) == 1, "one fusion allowed for the base smith");
    hero.subClass = HeroSubClass.ANVIL_MASTER;
    check(Smithing.maxFusions(hero) == 2, "anvil master can fuse twice");
    Smithing.fuse(hero, main, dagger);
    check(main.RCH == 2 && main.fusedNames.contains(",") , "second fusion keeps reach and lists both (" + main.fusedNames + ")");
    hero.subClass = HeroSubClass.NONE;

    // reinforcement
    int base = 0, reinf = 0;
    main.reinforced = 0;
    for (int i = 0; i < 5000; i++) base += main.damageRoll(hero);
    main.reinforced = 2;
    for (int i = 0; i < 5000; i++) reinf += main.damageRoll(hero);
    check(Math.abs((reinf - base)/5000f - 2) < 0.4f, "reinforcing adds about +2 damage per hit (" + (reinf-base)/5000f + ")");
    check(Smithing.reinforceCost(hero, main) == 16, "reinforce cost at 2 is 16");

    // save and load a fused weapon
    com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
    main.storeInBundle(b);
    MeleeWeapon back = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sword();
    back.restoreFromBundle(b);
    check(back.RCH == 2 && back.fusions == 2 && back.reinforced == 2 && back.fusedNames.equals(main.fusedNames), "fusion survives a save");
    check(Math.abs(back.DLY - main.DLY) < 0.001f && Math.abs(back.ACC - main.ACC) < 0.001f, "fused speed and accuracy survive a save");
    check(!back.info().contains("NO TEXT") && back.info().contains("2"), "fused weapon info shows anvil work");

    // heat save
    b = new com.watabou.utils.Bundle(); h.set(77); h.storeInBundle(b);
    Heat h2 = new Heat(); h2.restoreFromBundle(b);
    check(Math.abs(h2.heat() - 77) < 0.01f, "heat survives a save");

    // --- scrap
    check(Smithing.smeltValue(hero, new com.shatteredpixel.shatteredpixeldungeon.items.armor.PlateArmor()) == 12, "smelting plate armor gives 12 scrap");
    int before = Smithing.scrap(hero);
    check(Smithing.spendScrap(hero, 3) && Smithing.scrap(hero) == before - 3, "spending scrap");
    check(!Smithing.spendScrap(hero, 999), "can't spend more scrap than you have");
    Smithing.giveScrap(hero, 5);
    check(Smithing.scrap(hero) == before + 2, "giving scrap stacks");
    Dungeon.level.heaps.remove(hero.pos);
    com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Golem golem = new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Golem();
    golem.pos = hero.pos;
    Field gsf2 = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("scene"); gsf2.setAccessible(true);
    Object keep = gsf2.get(null); gsf2.set(null, null);
    Smithing.onKill(hero, golem);
    gsf2.set(null, keep);
    check(Dungeon.level.heaps.get(golem.pos) != null && Dungeon.level.heaps.get(golem.pos).peek() instanceof Scrap, "golems drop scrap for the smith");

    // --- other heroes are unaffected
    gsf2.set(null, null);
    startRun(HeroClass.WARRIOR, 1);
    check(Smithing.onHeroAttack(Dungeon.hero, rat, 10) == 10 && Dungeon.hero.buff(Heat.class) == null, "other heroes get no heat");

    // --- texts
    String[] keys = new String[0];
    for (Talent t : Talent.values()){
      if (t.ordinal() >= Talent.HOT_MEAL.ordinal() && t.ordinal() <= Talent.PHOENIX_HEART.ordinal()){
        check(!t.title().contains("NO TEXT") && !t.desc().contains("NO TEXT"), "talent text " + t.name());
      }
    }
    for (ArmorAbility ab : HeroClass.SMITH.armorAbilities()) check(!ab.name().contains("NO TEXT") && !ab.desc().contains("NO TEXT"), "ability text " + ab.name());
    for (HeroSubClass sc : HeroClass.SMITH.subClasses()) check(!sc.title().contains("NO TEXT") && !sc.desc().contains("NO TEXT"), "subclass text " + sc.title());
    for (Item i : new Item[]{new PortableAnvil(), new Scrap(), new SmithHammer(), new SmithArmor(), new com.shatteredpixel.shatteredpixeldungeon.items.remains.BentTongs()}){
      check(!i.name().contains("NO TEXT") && !i.desc().contains("NO TEXT"), "item text " + i.name());
    }
    check(!HeroClass.SMITH.title().contains("NO TEXT") && HeroClass.SMITH.desc().split("\n\n").length == 4, "class text has 4 paragraphs");
    System.out.println(fails == 0 ? "ALL SMITH CHECKS PASSED" : fails + " FAILED");
    System.exit(fails == 0 ? 0 : 1);
  }
}
