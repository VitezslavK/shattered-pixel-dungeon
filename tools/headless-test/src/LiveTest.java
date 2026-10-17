import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.*;
import com.shatteredpixel.shatteredpixeldungeon.online.*;
import com.watabou.noosa.Game;
import com.watabou.utils.Random;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

//one "phone" in a live link test: run with a role (A1, B, A2, B2) and its own prefs folder
public class LiveTest {
  static int fails = 0;
  static void check(boolean ok, String what){ System.out.println((ok ? "OK   " : "FAIL ") + what); if (!ok) fails++; }
  static void poll() throws Exception {
    CompletableFuture<Boolean> f = new CompletableFuture<>();
    LiveLink.poll(() -> f.complete(true));
    f.get(20, TimeUnit.SECONDS);
  }
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

  public static void main(String[] a) throws Exception {
    String role = a[0];
    File prefsDir = new File(a[1]); prefsDir.mkdirs();
    Gdx.files = new Lwjgl3Files();
    Gdx.net = new Lwjgl3Net(new Lwjgl3ApplicationConfiguration());
    Gdx.app = (Application) Proxy.newProxyInstance(LiveTest.class.getClassLoader(), new Class[]{Application.class}, (p,m,args)->{
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
    Gdx.gl = Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) Proxy.newProxyInstance(LiveTest.class.getClassLoader(), new Class[]{com.badlogic.gdx.graphics.GL20.class, com.badlogic.gdx.graphics.GL30.class}, (p,m,args)->{
      Class<?> r = m.getReturnType(); if (r==boolean.class) return false; if (r==int.class) return 1; if (r==long.class) return 0L; if (r==float.class) return 0f; if (r==String.class) return ""; return null; });
    com.watabou.utils.FileUtils.setDefaultFileProperties(Files.FileType.Absolute, prefsDir.getAbsolutePath()+"/");
    Game.version = "3.3.8"; Game.versionCode = 896;
    Messages.setup(Languages.CZECH);
    File roomFile = new File(prefsDir.getParentFile(), "room.txt");

    switch (role){
      case "A1": {
        SPDSettings.put("online_code", "TESTLA"); OnlineService.playerName("LiveA");
        String room = LiveLink.createRoom();
        java.nio.file.Files.writeString(roomFile.toPath(), room);
        check(room.matches("LIVE-[A-Z0-9]{6}"), "A created room " + room);
        startRun(HeroClass.WARRIOR, 3);
        check(LiveLink.activeRun(), "A's run belongs to the room (seed = room code)");
        LiveLink.tick(Dungeon.hero);
        Thread.sleep(3000);
        check(OnlineService.lastError == null, "A's join event accepted (" + OnlineService.lastError + ")");
        poll();
        break; }
      case "B": {
        SPDSettings.put("online_code", "TESTLB"); OnlineService.playerName("LiveB");
        String room = java.nio.file.Files.readString(roomFile.toPath());
        check(LiveLink.joinRoom(room.substring(5).toLowerCase()), "B joins with the bare code typed in lowercase");
        check(LiveLink.room().equals(room) && SPDSettings.customSeed().equals(room), "B's seed is set to the room");
        startRun(HeroClass.MAGE, 2);
        long seedB = Dungeon.seed;
        check(LiveLink.activeRun(), "B's run belongs to the room");
        LiveLink.tick(Dungeon.hero);
        Thread.sleep(3000);
        poll();
        LiveLink.Friend fa = null;
        for (LiveLink.Friend f : LiveLink.friends()) if (f.code.equals("TESTLA")) fa = f;
        check(fa != null, "B sees A in the room");
        if (fa != null){
          check(fa.name.equals("LiveA") && fa.heroClass == HeroClass.WARRIOR && fa.depth == 3, "B sees A's name, class and floor (" + fa.name + " " + fa.heroClass + " floor " + fa.depth + ")");
          check(LiveLink.canGift(), "B can send a gift");
          check(LiveLink.sendGift(fa, true), "B sends help to A");
          check(!LiveLink.canGift() && !LiveLink.sendGift(fa, false), "second gift on the same floor is refused");
          Dungeon.depth = 3;
          check(LiveLink.sendGift(fa, false), "B sends mischief to A from the next floor");
        }
        Thread.sleep(3000);
        check(OnlineService.lastError == null, "B's events accepted (" + OnlineService.lastError + ")");
        break; }
      case "A2": {
        startRun(HeroClass.WARRIOR, 3);
        Dungeon.hero.HP = Dungeon.hero.HT / 2;
        int hpBefore = Dungeon.hero.HP;
        int mobsBefore = Dungeon.level.mobs.size();
        poll();
        LiveLink.tick(Dungeon.hero);
        check(Dungeon.hero.HP > hpBefore, "A received help: HP " + hpBefore + " -> " + Dungeon.hero.HP);
        check(Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier.class) != null, "A got a shield");
        check(Dungeon.level.mobs.size() == mobsBefore + 1, "A received mischief: a monster appeared (" + mobsBefore + " -> " + Dungeon.level.mobs.size() + ")");
        //a gift is only applied once, even after reading the room again
        int hpNow = Dungeon.hero.HP;
        poll(); LiveLink.tick(Dungeon.hero);
        check(Dungeon.hero.HP == hpNow && Dungeon.level.mobs.size() == mobsBefore + 1, "gifts are not applied twice");
        LiveLink.onRunEnd(false);
        Thread.sleep(3000);
        check(OnlineService.lastError == null, "A's death event accepted (" + OnlineService.lastError + ")");
        break; }
      case "B2": {
        startRun(HeroClass.MAGE, 3);
        poll();
        LiveLink.Friend fa = null;
        for (LiveLink.Friend f : LiveLink.friends()) if (f.code.equals("TESTLA")) fa = f;
        check(fa != null && fa.dead, "B sees that A died");
        LiveLink.leave();
        check(!LiveLink.inRoom() && SPDSettings.customSeed().isEmpty(), "leaving clears the room and the seed");
        break; }
    }
    System.out.println(fails == 0 ? "ROLE " + role + " PASSED" : "ROLE " + role + " " + fails + " FAILED");
    System.exit(fails == 0 ? 0 : 1);
  }
}
