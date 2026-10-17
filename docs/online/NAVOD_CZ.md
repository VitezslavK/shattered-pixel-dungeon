# Online duchové – co je potřeba založit

Hra funguje i bez serveru. Offline tě navštěvují duchové **tvých vlastních** minulých hrdinů.
Aby se objevovali duchové **ostatních hráčů** a fungovalo přivolání kamaráda, je potřeba malý server zdarma (Supabase).

## 1. Založ projekt v Supabase
1. Jdi na <https://supabase.com> a zaregistruj se (stačí GitHub účet).
2. **New project** → název např. `spd-mod`, heslo databáze si ulož, region **Central EU (Frankfurt)**.
3. Počkej asi 2 minuty, než se projekt vytvoří.

## 2. Vytvoř tabulku
1. V levém menu **SQL Editor** → **New query**.
2. Vlož celý obsah souboru `docs/online/supabase_setup.sql` a klikni na **Run**.
   - Vzniknou tabulky `ghosts` (duchové), `daily_scores` (denní žebříček) a `bones` (sdílené kosti).
     Hra do nich smí jen přidávat a číst, upravovat ani mazat nic nemůže.

## 3. Propoj hru se serverem
1. V Supabase: **Project Settings → API**.
2. Zkopíruj **Project URL** a klíč **anon public**. Pozor: nikdy ne `service_role`!
3. Ve složce `core/src/main/assets/` zkopíruj `online.properties.example` jako `online.properties` a vyplň:
   ```
   supabase_url=https://tvuj-projekt.supabase.co
   supabase_anon_key=tvuj-anon-klic
   ```
4. Soubor `online.properties` je v `.gitignore`, takže se klíč nedostane do repozitáře.
   Do sestavené hry (APK) se ale přibalí. Klíč `anon` je k tomu určený, bezpečnost hlídají pravidla v SQL.
5. Hru znovu sestav (nebo mi napiš a sestavím APK).

## Jak to funguje ve hře
- **Invaze duchů (PvP):** když hra skončí (výhra i smrt), tvůj hrdina se uloží jako duch: jméno, třída, úroveň, zbraň a zbroj.
  Na patrech od 3. výš se s 15% šancí objeví duch jiného hráče jako silný nepřítel.
  Když ho porazíš, získáš kopii jeho zbraně nebo zbroje (max. +3).
- **Přivolání kamaráda (co-op):** každý hrdina začíná s **duchovním zvoncem**.
  - Akce **KAMARÁD**: zadej kód kamaráda (svůj kód vidíš v popisu zvonce).
  - Akce **JMÉNO**: tvé jméno, které uvidí ostatní.
  - Akce **ZAZVONIT**: jednou za hru přivolá ducha kamarádova posledního hrdiny jako spojence (bez kódu přijde náhodný duch).
  - Akce **ŽEBŘÍČEK** (jen online): dnešní nejlepší výsledky denní výzvy.
- **Denní výzva jako závod:** všichni hrají stejný dungeon, výsledek se po konci nahraje do žebříčku.
- **Sdílené kosti:** když hrdina zemře, nechá po sobě jeden předmět. Ten se může objevit
  jako kostra s kořistí ve hře jiného hráče na podobném patře (může být prokletý!).

