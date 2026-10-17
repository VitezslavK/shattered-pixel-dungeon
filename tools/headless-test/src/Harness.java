import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.*;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

public class Harness {
  static boolean SIM = Boolean.getBoolean("sim"); static int SIMTURNS = Integer.getInteger("simturns", 150);
  static Object FAKE; static Field GSF;
  static volatile String cur = ""; static volatile long curT = System.currentTimeMillis();
  static Map<String,Integer> problems = new TreeMap<>();
  static void setField(String n, Object v) throws Exception { Field f = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField(n); f.setAccessible(true); f.set(FAKE, v); }
  static void setupTiles() throws Exception {
    String[] names = {"tiles","visualGrid","terrainFeatures","raisedTerrain","walls","wallBlocking","fog"};
    for (String n : names){
      try {
        Object v;
        switch (n){
          case "tiles": v = new com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTerrainTilemap(); break;
          case "visualGrid": v = new com.shatteredpixel.shatteredpixeldungeon.tiles.GridTileMap(); break;
          case "terrainFeatures": v = new com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap(Dungeon.level.plants, Dungeon.level.traps); break;
          case "raisedTerrain": v = new com.shatteredpixel.shatteredpixeldungeon.tiles.RaisedTerrainTilemap(); break;
          case "walls": v = new com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonWallsTilemap(); break;
          case "wallBlocking": v = new com.shatteredpixel.shatteredpixeldungeon.tiles.WallBlockingTilemap(); break;
          default: v = new com.shatteredpixel.shatteredpixeldungeon.tiles.FogOfWar(Dungeon.level.width(), Dungeon.level.height());
        }
        setField(n, v);
      } catch (Throwable t){ problem("SETUP "+n+" "+t+" @ "+(t.getStackTrace().length>0?t.getStackTrace()[0]:"")); }
    }
  }
  static void buildHero(HeroClass hc, int s){
    Hero h = Dungeon.hero;
    try {
      h.lvl = 30;
      HeroSubClass[] subs = hc.subClasses();
      h.subClass = subs[s % subs.length];
      Talent.initSubclassTalents(h);
      com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility[] ab = hc.armorAbilities();
      h.armorAbility = ab[(s/2) % ab.length];
      Talent.initArmorTalents(h);
      com.shatteredpixel.shatteredpixeldungeon.actors.hero.evolution.Evolution.onEvolve(h);
      for (LinkedHashMap<Talent,Integer> tier : h.talents){
        for (Talent t : new ArrayList<>(tier.keySet())){
          tier.put(t, t.maxPoints());
          try { Talent.onTalentUpgraded(h, t); } catch (Throwable e){ problem("TALENTUP "+t+" "+e+" @ "+(e.getStackTrace().length>0?e.getStackTrace()[0]:"")); }
        }
      }
      h.updateHT(true);
    } catch (Throwable e){
      StringBuilder sb=new StringBuilder(); for (StackTraceElement x : e.getStackTrace()) { if (sb.length()>800) break; sb.append("\n    at ").append(x); }
      problem("BUILDHERO "+hc+" "+e+sb);
    }
  }
  static void problem(String s){ problems.merge(s,1,Integer::sum); }
  static void check(String where, Object o, String m){
    try {
      Method mm = null;
      for (Class c=o.getClass(); c!=null && mm==null; c=c.getSuperclass()){
        try { mm = c.getDeclaredMethod(m); } catch (NoSuchMethodException e){}
      }
      if (mm==null || mm.getParameterCount()!=0) return;
      mm.setAccessible(true);
      Object r = mm.invoke(o);
      if (r instanceof String && ((String)r).contains(Messages.NO_TEXT_FOUND)) problem("NOTEXT "+Messages.lang()+" "+where+"."+m);
    } catch (InvocationTargetException e){
      Throwable t=e.getCause(); StackTraceElement[] st=t.getStackTrace();
      problem("EXC "+where+"."+m+": "+t+" @ "+(st.length>0?st[0]:""));
    } catch (Throwable t){ problem("EXC2 "+where+"."+m+": "+t); }
  }
  static void checkAll(String where, Object o){
    if (o instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff){
      try { if (((com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff)o).icon()==com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.NONE) return; } catch (Throwable t){}
    }
    for (String m : new String[]{"name","desc","info","description","toString","title","shortDesc","statsDesc","heroMessage","iconTextDisplay"}) check(where,o,m);
  }
  public static void main(String[] a) throws Exception {
    File prefsDir = new File(a[0]);
    Gdx.files = new Lwjgl3Files();
    Gdx.app = (Application) Proxy.newProxyInstance(Harness.class.getClassLoader(), new Class[]{Application.class}, (p,m,args)->{
      switch (m.getName()){
        case "getPreferences": return new Lwjgl3Preferences(new FileHandle(new File(prefsDir,(String)args[0])));
        case "getType": return Application.ApplicationType.Desktop;
        case "log": case "error": case "debug": if (args.length>1 && !String.valueOf(args[1]).isEmpty()) System.out.println("[log] "+args[0]+": "+args[1]); if (args.length>2 && args[2] instanceof Throwable) ((Throwable)args[2]).printStackTrace(System.out); return null;
        case "getVersion": return 0;
        case "hashCode": return 1; case "equals": return false; case "toString": return "app";
      }
      Class<?> r = m.getReturnType();
      if (r==boolean.class) return false; if (r==int.class||r==long.class) return 0; return null;
    });
    com.watabou.utils.FileUtils.setDefaultFileProperties(Files.FileType.Absolute, prefsDir.getAbsolutePath()+"/");
    com.badlogic.gdx.utils.GdxNativesLoader.load();
    Object glp = Proxy.newProxyInstance(Harness.class.getClassLoader(), new Class[]{com.badlogic.gdx.graphics.GL20.class, com.badlogic.gdx.graphics.GL30.class}, (p,m,args)->{
      Class<?> r = m.getReturnType();
      if (r==boolean.class) return false; if (r==int.class) return 1; if (r==long.class) return 0L; if (r==float.class) return 0f; if (r==String.class) return ""; return null;
    });
    Gdx.gl = Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) glp;
    Game.width = Game.height = 800;
    com.watabou.noosa.Camera.main = new com.watabou.noosa.Camera(0,0,800,800,2f);
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
            try { f.setAccessible(true); Constructor<?> k = f.getType().getDeclaredConstructor(); k.setAccessible(true); Object gv = k.newInstance(); f.set(gs, gv); if (f.getType() == com.watabou.noosa.Group.class) com.shatteredpixel.shatteredpixeldungeon.actors.Sim.groups.add((com.watabou.noosa.Group) gv); } catch (Throwable t){ }
          }
        }
      }
      try { Field members = com.watabou.noosa.Group.class.getDeclaredField("members"); members.setAccessible(true); members.set(gs, new ArrayList<>()); } catch (Throwable t){ System.out.println("members "+t); }
      { Field cs = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("cellSelector"); cs.setAccessible(true); cs.set(null, u.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector.class)); }
      { try { Field uc = com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.class.getDeclaredField("uiCamera"); uc.setAccessible(true); uc.set(Modifier.isStatic(uc.getModifiers()) ? null : gs, new com.watabou.noosa.Camera(0,0,800,800,2f)); } catch (Throwable t){ System.out.println("uicam "+t); } }
      Gdx.graphics = (Graphics) Proxy.newProxyInstance(Harness.class.getClassLoader(), new Class[]{Graphics.class}, (p,m,args)->{
        Class<?> r = m.getReturnType();
        if (r==boolean.class) return false; if (r==int.class) return 800; if (r==long.class) return 0L; if (r==float.class) return 1f; if (r==double.class) return 1d; return null;
      });
      {
        Object inv = u.allocateInstance(com.shatteredpixel.shatteredpixeldungeon.ui.InventoryPane.class);
        Field vis = com.watabou.noosa.Gizmo.class.getDeclaredField("visible"); vis.setAccessible(true); vis.setBoolean(inv, true);
        Field invf = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("inventory"); invf.setAccessible(true); invf.set(gs, inv);
        com.shatteredpixel.shatteredpixeldungeon.actors.Sim.inventory = inv;
        Field mem = com.watabou.noosa.Group.class.getDeclaredField("members"); mem.setAccessible(true); if (mem.get(gs) == null) mem.set(gs, new ArrayList<>());
      }
      Field gsf = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("scene"); gsf.setAccessible(true); gsf.set(null, gs); FAKE = gs;
      { Field mf = com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.class.getDeclaredField("mobs"); mf.setAccessible(true); com.shatteredpixel.shatteredpixeldungeon.actors.Sim.spriteGroup = (com.watabou.noosa.Group) mf.get(gs); } GSF = gsf;
      sf.set(g, gs);
    }
    Game.version = "3.3.8"; Game.versionCode = 900;
    int seeds = Integer.parseInt(a[1]);
    Thread main = Thread.currentThread();
    Thread wd = new Thread(()->{ while(true){ try{Thread.sleep(2000);}catch(Exception e){}
      if (System.currentTimeMillis()-curT > 20000){ System.out.println("HANG at "+cur); for (StackTraceElement e : main.getStackTrace()) System.out.println("    at "+e); System.out.println("=== PROBLEMS"); problems.forEach((k,v)->System.out.println(v+"x "+k)); System.exit(3);} }});
    wd.setDaemon(true); wd.start();

    // all classes in core jar
    List<Class<?>> classes = new ArrayList<>();
    try (ZipFile z = new ZipFile(a[2])){
      for (Enumeration<? extends ZipEntry> e=z.entries(); e.hasMoreElements();){
        String n=e.nextElement().getName();
        if (!n.endsWith(".class")) continue;
        try { classes.add(Class.forName(n.replace('/', '.').replace(".class",""), false, Harness.class.getClassLoader())); } catch (Throwable t){}
      }
    }

    for (Languages lang : (Boolean.getBoolean("enonly") ? new Languages[]{Languages.ENGLISH} : new Languages[]{Languages.ENGLISH, Languages.CZECH})){
      Messages.setup(lang);
      Map<String,Integer> levelCounts = new TreeMap<>();
      for (int s=0;s<seeds;s++){
        for (HeroClass hc : HeroClass.values()){
          if (System.getProperty("cls") != null && !hc.name().equals(System.getProperty("cls"))) continue;
          String ctx = "seed"+s+" "+hc;
          try {
            GamesInProgress.selectedClass = hc;
            Dungeon.seed = 1000L + s*7919L + hc.ordinal();
            Dungeon.daily = false;
            GamesInProgress.curSlot = 1;
            Dungeon.init();
            if (SIM) buildHero(hc, s);
            Dungeon.cavesPath = s%2; Dungeon.cityPath = (s/2)%2;
            if (s==0) for (Item i : Dungeon.hero.belongings) checkAll("start:"+hc+":"+i.getClass().getSimpleName(), i);
            if (s==0) for (Talent t : Talent.values()) { checkAll("talent:"+t, new Object(){ public String title(){return t.title();} public String desc(){return t.desc();} }); }
            for (int d=1; d<=Dungeon.LAST_DEPTH; d++){
              ctx = "seed"+s+" "+hc+" depth"+d;
              Dungeon.depth=d; Dungeon.branch=0; cur=ctx; curT=System.currentTimeMillis();
              Random.pushGenerator(Dungeon.seedCurDepth());
              Level l;
              GSF.set(null, null);
              try { l = Dungeon.newLevel(); } finally { Random.popGenerator(); }
              Dungeon.level = l;
              if (SIM){
                GSF.set(null, FAKE);
                for (com.watabou.noosa.Group g : com.shatteredpixel.shatteredpixeldungeon.actors.Sim.groups) g.clear();
                Dungeon.level = l;
                com.watabou.utils.PathFinder.setMapSize(l.width(), l.height());
                com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.setupVariance(l.map.length, Dungeon.seedCurDepth());
                setupTiles();
                cur=ctx+" switch"; curT=System.currentTimeMillis();
                Dungeon.switchLevel(l, -1);
                com.shatteredpixel.shatteredpixeldungeon.actors.Sim.ensureSprites();
                String[] extra = {"items.potions.brews.BindingBrew","items.potions.brews.PlagueBrew","items.potions.elixirs.ElixirOfArcanum","items.potions.elixirs.ElixirOfCrystalSkin","items.EssenceOfAscension","items.quest.VoidShard","items.quest.PlagueGland","items.wands.WandOfPlague","items.rings.RingOfBloodlust","items.weapon.melee.VoidBlade"};
                for (String ex : extra){
                  try { Item gi = (Item) Class.forName("com.shatteredpixel.shatteredpixeldungeon."+ex).getDeclaredConstructor().newInstance(); gi.identify(); gi.collect(); } catch (Throwable t){ problem("GENEXTRA "+ex+" "+t); }
                }
                try { new com.shatteredpixel.shatteredpixeldungeon.items.RuneTablet().doPickUp(Dungeon.hero, Dungeon.hero.pos); } catch (Throwable t){
                  StringBuilder sb=new StringBuilder(); for (StackTraceElement x : t.getStackTrace()) { if (sb.length()>800) break; sb.append("\n    at ").append(x); }
                  if (!String.valueOf(sb).contains(".ui.") && !String.valueOf(t).contains("Game.platform")) problem("RUNETABLET "+t+sb);
                }
                for (int k = 0; k < 4; k++){
                  try { Item gi = com.shatteredpixel.shatteredpixeldungeon.items.Generator.random(); gi.identify(); if (!gi.collect()) break; } catch (Throwable t){ problem("GENITEM "+t); }
                }
                cur=ctx+" sim"; curT=System.currentTimeMillis();
                String r = com.shatteredpixel.shatteredpixeldungeon.actors.Sim.run(SIMTURNS);
                for (Map.Entry<String,Integer> e : com.shatteredpixel.shatteredpixeldungeon.actors.Sim.errors.entrySet()) problems.merge("ACT "+e.getKey(), e.getValue(), Integer::sum);
                com.shatteredpixel.shatteredpixeldungeon.actors.Sim.errors.clear();
                if (!r.startsWith("turns="+SIMTURNS)) problem("SIM "+l.getClass().getSimpleName()+" "+r);
                cur=ctx+" save"; curT=System.currentTimeMillis();
                GSF.set(null, null);
                Dungeon.saveAll();
                Dungeon.loadGame(GamesInProgress.curSlot);
                Level l2 = Dungeon.loadLevel(GamesInProgress.curSlot);
                Dungeon.switchLevel(l2, Dungeon.hero.pos);
                l = l2;
              }
              levelCounts.merge(d+":"+l.getClass().getSimpleName(),1,Integer::sum);
              for (Mob m : l.mobs) checkAll("mob:"+m.getClass().getName(), m);
              for (com.shatteredpixel.shatteredpixeldungeon.items.Heap h : l.heaps.valueList()) for (Item i : h.items) checkAll("item:"+i.getClass().getName(), i);
              if (l.event != null) checkAll("event:"+l.event, l.event);
            }
          } catch (Throwable t){
            StringBuilder sb=new StringBuilder(); for (StackTraceElement e : t.getStackTrace()) { if (sb.length()>600) break; sb.append("\n    at ").append(e); }
            for (Throwable c = t.getCause(); c != null; c = c.getCause()) sb.append("\n  caused by: ").append(c);
            problem("LEVELGEN "+t+sb);
            if (t instanceof java.io.IOException){
              try { java.nio.file.Files.copy(new File(prefsDir, GamesInProgress.gameFile(GamesInProgress.curSlot)).toPath(), new File(prefsDir, "../badsave.dat").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING); } catch (Throwable t2){ System.out.println("copy failed "+t2); }
            }
            System.out.println("crash at "+ctx);
          }
        }
      }
      if (lang==Languages.ENGLISH) levelCounts.forEach((k,v)->System.out.println("  "+k+" x"+v));

      // generic instantiation of all concrete classes
      Dungeon.depth = 20;
      for (Class<?> c : classes){
        if (Modifier.isAbstract(c.getModifiers()) || c.isInterface() || c.isEnum() || c.isAnonymousClass() || c.isMemberClass() && !Modifier.isStatic(c.getModifiers())) continue;
        String n = c.getName();
        if (!(n.contains(".items.") || n.contains(".actors.") || n.contains(".mechanics.") || n.contains(".plants."))) continue;
        if (n.contains(".sprites.") || n.contains(".ui.") || n.contains(".windows.") || n.contains(".scenes.")) continue;
        Object o;
        try { Constructor<?> k = c.getDeclaredConstructor(); k.setAccessible(true); o = k.newInstance(); } catch (Throwable t){ continue; }
        checkAll(n, o);
      }
      for (HeroSubClass sc : HeroSubClass.values()) checkAll("subclass:"+sc, new Object(){ public String title(){return sc.title();} public String desc(){return sc.desc();} public String shortDesc(){return sc.shortDesc();} });
      for (HeroClass hc : HeroClass.values()) checkAll("class:"+hc, new Object(){ public String title(){return hc.title();} public String desc(){return hc.desc();} public String shortDesc(){return hc.shortDesc();} });
    }
    System.out.println("USED: "+com.shatteredpixel.shatteredpixeldungeon.actors.Sim.used.size()+" distinct item actions"); com.shatteredpixel.shatteredpixeldungeon.actors.Sim.used.forEach((k,v)->System.out.println("USEDKEY "+v+" "+k));
    System.out.println("=== PROBLEMS: "+problems.size());
    problems.forEach((k,v)->System.out.println(v+"x "+k));
    System.exit(0);
  }
}
