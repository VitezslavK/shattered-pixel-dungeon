import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GhostInvader;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.GhostAlly;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Longsword;
import com.shatteredpixel.shatteredpixeldungeon.messages.*;
import com.shatteredpixel.shatteredpixeldungeon.online.*;
import com.watabou.noosa.Game;
import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

//end-to-end test of the online features against the real Supabase project
public class OnlineTest {
  static int fails = 0;
  static void check(boolean ok, String what){ System.out.println((ok ? "OK   " : "FAIL ") + what); if (!ok) fails++; }
  static <T> T await(java.util.function.Consumer<java.util.function.Consumer<T>> call) throws Exception {
    CompletableFuture<T> f = new CompletableFuture<>();
    call.accept(f::complete);
    return f.get(20, TimeUnit.SECONDS);
  }
  static void waitUploads() throws Exception { Thread.sleep(4000); }

  public static void main(String[] a) throws Exception {
    File prefsDir = new File(a[0]); prefsDir.mkdirs();
    Gdx.files = new Lwjgl3Files();
    Gdx.net = new Lwjgl3Net(new Lwjgl3ApplicationConfiguration());
    Gdx.app = (Application) Proxy.newProxyInstance(OnlineTest.class.getClassLoader(), new Class[]{Application.class}, (p,m,args)->{
      switch (m.getName()){
        case "getPreferences": return new Lwjgl3Preferences(new FileHandle(new File(prefsDir,(String)args[0])));
        case "getType": return Application.ApplicationType.Desktop;
        case "postRunnable": ((Runnable)args[0]).run(); return null;
        case "log": case "error": case "debug": System.out.println("[log] "+args[0]+": "+args[1]); return null;
        case "hashCode": return 1; case "equals": return false; case "toString": return "app";
      }
      Class<?> r = m.getReturnType(); if (r==boolean.class) return false; if (r==int.class||r==long.class) return 0; return null;
    });
    com.watabou.utils.FileUtils.setDefaultFileProperties(Files.FileType.Absolute, prefsDir.getAbsolutePath()+"/");
    com.badlogic.gdx.utils.GdxNativesLoader.load();
    Gdx.gl = Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) Proxy.newProxyInstance(OnlineTest.class.getClassLoader(), new Class[]{com.badlogic.gdx.graphics.GL20.class, com.badlogic.gdx.graphics.GL30.class}, (p,m,args)->{
      Class<?> r = m.getReturnType(); if (r==boolean.class) return false; if (r==int.class) return 1; if (r==long.class) return 0L; if (r==float.class) return 0f; if (r==String.class) return ""; return null; });
    Game.version = "3.3.8"; Game.versionCode = 896;
    Messages.setup(Languages.ENGLISH);

    check(OnlineService.enabled(), "online.properties found and valid");
    SPDSettings.put("online_code", "TESTQA");
    OnlineService.playerName("SPDTEST");
    check(OnlineService.friendCode().equals("TESTQA") && OnlineService.playerName().equals("SPDTEST"), "identity set");

    //1) a run ends: ghost + bones are uploaded, using the hero's real data
    GamesInProgress.selectedClass = HeroClass.NECROMANCER;
    GamesInProgress.curSlot = 1;
    Dungeon.seed = 12345; Dungeon.init();
    Dungeon.depth = 31; Dungeon.hero.lvl = 30;
    Longsword sword = new Longsword(); sword.level(20); sword.identify();
    Dungeon.hero.belongings.weapon = sword;
    GhostPool.onRunEnd(false);
    waitUploads();
    check(OnlineService.lastError == null, "ghost + bones upload accepted by the server (lastError=" + OnlineService.lastError + ")");

    //2) daily score upload, on a day nobody looks at
    OnlineExtras.uploadDailyScore("2000-01-01", 4321, 31, false, HeroClass.NECROMANCER);
    waitUploads();
    check(OnlineService.lastError == null, "daily score upload accepted (lastError=" + OnlineService.lastError + ")");
    check(OnlineExtras.utcDay(0L).equals("1970-01-01"), "daily dates are UTC");

    //3) download: by friend code, recent list, bones, leaderboard
    ArrayList<GhostSnapshot> friend = await(cb -> OnlineService.fetchFriend("TESTQA", cb::accept));
    check(!friend.isEmpty(), "fetch ghost by friend code");
    if (!friend.isEmpty()){
      GhostSnapshot g = friend.get(0);
      check(g.playerName.equals("SPDTEST"), "ghost name round-trips");
      check(g.heroClass == HeroClass.NECROMANCER, "ghost class round-trips");
      check(g.depth == 31, "floor 31 accepted");
      check(g.weaponLvl == 15, "weapon level clamped to 15 (was " + g.weaponLvl + ")");
      check(g.createWeapon() instanceof Longsword, "ghost weapon can be recreated");
      GhostInvader inv = GhostInvader.create(g, 20);
      check(inv.HT > 0 && inv.name() != null, "invading ghost created: " + inv.name() + " HT=" + inv.HT);
      GhostAlly ally = GhostAlly.create(g);
      check(ally.HT > 0, "summoned ally created HT=" + ally.HT);
    }
    ArrayList<GhostSnapshot> recent = await(cb -> OnlineService.fetchRecent(cb::accept));
    boolean inRecent = false; for (GhostSnapshot g : recent) if ("TESTQA".equals(g.friendCode)) inRecent = true;
    check(inRecent, "ghost appears in the recent players list (" + recent.size() + " ghosts)");
    ArrayList<OnlineExtras.BonesEntry> bones = await(cb -> OnlineExtras.fetchBones(cb::accept));
    boolean boneOk = false;
    for (OnlineExtras.BonesEntry b : bones){ Item it = b.create(); if (b.depth == 31 && it != null) boneOk = true; }
    check(boneOk, "bones downloaded and turned into an item (" + bones.size() + " bones)");
    ArrayList<OnlineExtras.Score> scores = await(cb -> OnlineExtras.fetchDailyTop("2000-01-01", cb::accept));
    boolean scoreOk = false; for (OnlineExtras.Score s : scores) if (s.playerName.equals("SPDTEST") && s.score == 4321) scoreOk = true;
    check(scoreOk, "daily leaderboard returns the score");

    //4) another player's ghost invades: the pool skips our own ghosts and picks remote ones
    SPDSettings.put("online_code", "TESTQB");
    GhostPool.refresh();
    Thread.sleep(5000);
    GhostSnapshot picked = null;
    for (int i = 0; i < 30 && (picked == null || !"TESTQA".equals(picked.friendCode)); i++) picked = GhostPool.pick(20);
    check(picked != null, "ghost pool has ghosts for invasions");
    GhostSnapshot pickedBones = null;
    OnlineExtras.BonesEntry pb = GhostPool.pickBones(31);
    check(pb != null, "bones pool returns bones near floor 31");
    check(OnlineService.lastError == null, "no failed requests (lastError=" + OnlineService.lastError + ")");

    System.out.println(fails == 0 ? "ALL ONLINE CHECKS PASSED" : (fails + " CHECKS FAILED"));
    System.exit(fails == 0 ? 0 : 1);
  }
}
