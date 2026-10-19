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

//checks the golden items, recipes, altars and shopkeeper changes without a screen
public class GoldTest {
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

  public static void main(String[] a) throws Exception {
        File prefsDir = new File(a[0]); prefsDir.mkdirs();
    Gdx.files = new Lwjgl3Files();
    Gdx.net = new Lwjgl3Net(new Lwjgl3ApplicationConfiguration());
    Gdx.app = (Application) Proxy.newProxyInstance(GoldTest.class.getClassLoader(), new Class[]{Application.class}, (p,m,args)->{
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
    Gdx.gl = Gdx.gl20 = (com.badlogic.gdx.graphics.GL20) Proxy.newProxyInstance(GoldTest.class.getClassLoader(), new Class[]{com.badlogic.gdx.graphics.GL20.class, com.badlogic.gdx.graphics.GL30.class}, (p,m,args)->{
      Class<?> r = m.getReturnType(); if (r==boolean.class) return false; if (r==int.class) return 1; if (r==long.class) return 0L; if (r==float.class) return 0f; if (r==String.class) return ""; return null; });
    com.watabou.utils.FileUtils.setDefaultFileProperties(Files.FileType.Absolute, prefsDir.getAbsolutePath()+"/");
    Game.version = "3.3.8"; Game.versionCode = 896;
    Messages.setup(Languages.ENGLISH);

    Dungeon.hero = null;
    startRun(HeroClass.WARRIOR, 2);
    Hero hero = Dungeon.hero;

    // --- where altars appear
    for (int d : new int[]{2,6,11,16,21,26}) check(GoldenAltar.appearsOn(d), "altar appears on floor " + d);
    for (int d : new int[]{1,3,5,10,25}) check(!GoldenAltar.appearsOn(d), "no altar on floor " + d);
    int found = 0, tried = 0;
    for (int d : new int[]{2,6,11,16,21,26}){
      for (int seed = 0; seed < 4; seed++){
        Dungeon.seed = 1000 + seed * 77 + d;
        Dungeon.depth = d;
        Random.pushGenerator(Dungeon.seedCurDepth());
        Level l = Dungeon.newLevel();
        Random.popGenerator();
        tried++;
        for (Mob m : l.mobs) if (m instanceof GoldenAltar){ found++;
          check(l.distance(m.pos, l.entrance()) >= 2, "altar not on the entrance (floor " + d + ")"); }
      }
    }
    check(found == tried, "an altar was placed on every region start level (" + found + "/" + tried + ")");
    startRun(HeroClass.WARRIOR, 2); hero = Dungeon.hero;

    // --- recipes
    check(Recipe.usableInRecipe(ident(new Dagger())), "melee weapons can go into the alchemy pot");
    Item dagger = ident(new Dagger()); dagger.level(2);
    ArrayList<Item> in = list(new GoldIngot().quantity(2), dagger);
    Recipe r = only(in, GildRecipe.class);
    check(r != null, "2 ingots + dagger finds the gilding recipe");
    Item out = r.brew(in);
    check(out instanceof GildedDagger && out.level() == 2, "gilding a +2 dagger gives a +2 gilded dagger (" + out + ")");
    check(in.get(0).quantity() == 0 && in.get(1).quantity() == 0, "gilding used up the ingots and the weapon");
    out = only(list(new GoldIngot().quantity(4), ident(new Sword())), GildRecipe.class).sampleOutput(list(new GoldIngot().quantity(4), ident(new Sword())));
    check(out instanceof GoldenSabre, "4 ingots + sword gives a golden sabre");
    check(only(list(new GoldIngot().quantity(3), ident(new Sword())), GildRecipe.class) == null, "3 ingots are not enough for a tier 3 weapon");
    out = only(list(new GoldIngot().quantity(6), ident(new Greataxe())), GildRecipe.class).sampleOutput(list(new GoldIngot().quantity(6), ident(new Greataxe())));
    check(out instanceof MidasHammer, "6 ingots + greataxe gives Midas' hammer");
    check(only(list(new GoldIngot().quantity(2), ident(new GildedDagger())), GildRecipe.class) == null, "a golden weapon can't be gilded again");
    check(outputs(list(new GoldIngot())).contains(LuckyCoin.class), "1 ingot makes lucky coins");
    check(outputs(list(new GoldIngot(), new Bomb())).contains(GoldBomb.class), "ingot + bomb makes a gold bomb");
    ArrayList<Class<?>> heal = outputs(list(new GoldIngot().quantity(4), new PotionOfHealing().identify()));
    check(heal.contains(ElixirOfGoldenSkin.class) && heal.contains(GoldenHeart.class), "4 ingots + healing offers the elixir and the golden heart");
    check(outputs(list(new GoldIngot().quantity(4), new ScrollOfIdentify().identify())).contains(GoldenIdol.class), "4 ingots + identify makes the golden idol");
    check(outputs(list(new GoldIngot().quantity(4), new ScrollOfRemoveCurse().identify())).contains(MerchantsSeal.class), "4 ingots + remove curse makes the merchant's seal");

    // --- talismans and prices
    Item potion = new PotionOfHealing();
    int base = Shopkeeper.sellPrice(potion);
    new MerchantsSeal().collect();
    int sealed = Shopkeeper.sellPrice(potion);
    check(sealed == Math.round(base * 0.85f), "merchant's seal: " + base + " -> " + sealed + " (15% off)");
    new GoldenIdol().collect();
    check(Math.abs(GoldenIdol.goldMultiplier() - 1.2f) < 0.001f, "golden idol: gold x1.2");
    new GoldenHeart().collect();
    check(GoldenHeart.healFor(100) == 10, "golden heart: 100 gold heals 10");
    com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff.affect(hero, Fortune.class, 10f);
    check(Fortune.lootMultiplier(hero) == 1.5f && Fortune.goldMultiplier(hero) == 1.25f, "fortune buff multipliers");

    // --- altar offers, prices and saving
    GoldenAltar altar = GoldenAltar.create();
    com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
    altar.storeInBundle(b);
    GoldenAltar back = new GoldenAltar();
    back.restoreFromBundle(b);
    java.lang.reflect.Field of = GoldenAltar.class.getDeclaredField("offers"); of.setAccessible(true);
    ArrayList<?> offers = (ArrayList<?>) of.get(back);
    check(offers.size() == 3, "altar keeps its 3 offers through a save");
    check(offers.get(0) instanceof GildedDagger, "floor 2 altar offers the gilded dagger (" + offers.get(0) + ")");
    check(GoldenAltar.price(1) < GoldenAltar.price(0) && GoldenAltar.price(0) < GoldenAltar.price(2), "altar prices: consumable < gear < talisman");
    Dungeon.depth = 26;
    check(GoldItems.equipment(26) instanceof MidasHammer, "deep altars offer Midas' hammer");

    // --- shop stock and shopkeeper state
    Dungeon.depth = 6;
    java.lang.reflect.Method gen = ShopRoom.class.getDeclaredMethod("generateItems"); gen.setAccessible(true);
    ArrayList<?> stock = (ArrayList<?>) gen.invoke(null);
    boolean box = false, golden = false;
    for (Object o : stock){ if (o instanceof MysteryBox) box = true; if (o.getClass().getPackage().getName().endsWith(".gold") && !(o instanceof MysteryBox) || o instanceof GoldenIdol || o instanceof MerchantsSeal || o instanceof GoldenHeart) golden = true; }
    check(box && golden, "the shop stocks a mystery box and a golden item");
    Shopkeeper sk = new Shopkeeper(); sk.priceMult = 0.8f; sk.haggled = true;
    b = new com.watabou.utils.Bundle(); sk.storeInBundle(b);
    Shopkeeper sk2 = new Shopkeeper(); sk2.restoreFromBundle(b);
    check(sk2.priceMult == 0.8f && sk2.haggled, "haggling result survives a save");

    // --- texts
    for (Item i : new Item[]{new GoldIngot(), new GildedDagger(), new GoldenSabre(), new MidasHammer(), new GoldBomb(), new LuckyCoin(),
        new ElixirOfGoldenSkin(), new MysteryBox(), new GoldenIdol(), new MerchantsSeal(), new GoldenHeart()}){
      check(!i.name().contains("NO TEXT") && !i.desc().contains("NO TEXT"), "texts for " + i.name());
    }
    System.out.println(fails == 0 ? "ALL GOLD CHECKS PASSED" : fails + " FAILED");
    System.exit(fails == 0 ? 0 : 1);
  }

  static Item ident(Item i){ i.identify(); i.cursed = false; i.cursedKnown = true; return i; }
  static ArrayList<Item> list(Item... items){ return new ArrayList<>(Arrays.asList(items)); }
  static Recipe only(ArrayList<Item> in, Class<?> type){
    for (Recipe r : Recipe.findRecipes(in)) if (type.isInstance(r)) return r;
    return null;
  }
  static ArrayList<Class<?>> outputs(ArrayList<Item> in){
    ArrayList<Class<?>> out = new ArrayList<>();
    for (Recipe r : Recipe.findRecipes(in)) out.add(r.sampleOutput(in).getClass());
    return out;
  }
}
