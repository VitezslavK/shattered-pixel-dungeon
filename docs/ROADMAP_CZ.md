# Roadmapa úprav Shattered Pixel Dungeon

Základ: SPD v3.3.8 (Java + libGDX). Cíl: delší dungeon, nová postava, evoluce podtříd, nový ending a víc náhody, aby se hry tolik neopakovaly.

Pořadí fází je zvolené tak, aby každá fáze šla sestavit a hrát sama o sobě.

---

## Fáze 0 – Základy (krátké)

- Sestavení desktop verze (JDK 21 přes Homebrew, `./gradlew desktop:debug`).
- **Kompatibilita uložených her:** nové verze budou mít vlastní číslo verze; rozehrané hry ze staré verze se buď dohrají po staru, nebo se smažou – rozhodneme (doporučuji nepodporovat staré savy, zjednoduší to práci).
- **Grafika:** pro začátek dočasné sprity vytvořené přebarvením existujících (skript v Pythonu), skutečný pixel art se doplní později.
- **Texty:** angličtina (základ) + čeština. Ostatní jazyky automaticky spadnou na angličtinu.

---

## Fáze 1 – Nová postava: Nekromant

Žádná z šesti tříd nestaví hru na vlastních spojencích – to je nejvýraznější díra v nabídce.

**Startovní vybavení**
- Zbraň: Srp (nová slabá zbraň 1. úrovně, „mrtvé“ zásahy dávají duše navíc).
- **Lucerna duší** (artefakt třídy, obdoba Svaté knihy Clerica): sbírá duše ze zabitých nepřátel. Akce:
  - *Vyvolat služebníka* – kostlivý spojenec, síla roste s úrovní hrdiny a lucerny, následuje hrdinu mezi patry, dá se mu rozkázat cíl.
  - *Pohltit duši* – vymění náboj za léčení.
- Identifikované předměty na startu: Lektvar Toxického plynu, Svitek Strachu.

**Podtřídy (po 2. bossovi)**
| Podtřída | Styl hry |
|---|---|
| **Pán kostí** | Armáda: až 2–3 služebníci, služebníci při smrti explodují kostmi, posílení služebníků. |
| **Žnec duší** | Sólo bojovník: duše spaluje sám na sebe – vysávání života, poškození navíc, zabití obnoví tah. |

**Talenty**: T1 (4), T2 (5), T3 třída (2) + podtřída (3 + 3), T4 schopnosti zbroje – stejná struktura jako ostatní třídy.

**Schopnosti zbroje (T4)**
1. *Armáda mrtvých* – na chvíli vyvolá několik dočasných kostlivců.
2. *Mrtvolná exploze* – zvolená mrtvola/služebník vybuchne nekrotickou energií.
3. *Výměna duší* – prohodí se s cílem a ukradne mu část života.

Odemčení: zabít 1. bosse s alespoň 1 živým spojencem (nebo jako Cleric dosáhnout 10. patra – vybereme).

---

## Fáze 2 – Evoluce podtříd (5. tier)

Každá podtřída se v pozdní hře vyvine do vyšší formy s **unikátní pasivní schopností** a **3 novými talenty 5. tieru**.

- Spouštěč: nový předmět **Esence vzestupu**, kterou upustí boss na 25. patře (Yog-Dzewa). Použitím se podtřída vyvine (obdoba Tenguovy masky).
- Strop úrovně hrdiny se zvedne z 30 na **36**, 5. tier talentů od úrovně 31.

| Třída | Podtřída → Evoluce | Nápad na pasivku |
|---|---|---|
| Warrior | Berserker → **Krvavý vévoda** | Vztek se nevybíjí po zuřivosti úplně; zabití během zuřivosti ji prodlouží. |
| | Gladiator → **Mistr čepelí** | Combo neklesá mimo boj; dokončovací údery se dají řetězit. |
| Mage | Battlemage → **Čarobijec** | Každý 3. úder holí zdarma sešle hůlku na poloviční sílu. |
| | Warlock → **Lich** | Označení nepřátelé po smrti vstanou jako přízraky; jednou za patro přežije smrtelný zásah. |
| Rogue | Assassin → **Fantom** | Zabití ze stealthu hrdinu znovu zneviditelní. |
| | Freerunner → **Větroběžec** | Při plné hybnosti prochází skrz nepřátele a uhýbá střelám. |
| Huntress | Sniper → **Ostrostřelec** | Označené výstřely se odrážejí na další cíl. |
| | Warden → **Arcidruid** | Rostliny občas vyrostou jako dočasný stromový strážce. |
| Duelist | Champion → **Válečný mistr** | Třetí zbraňový slot, schopnosti obou zbraní najednou. |
| | Monk → **Velmistr** | Postoje (útok / obrana / pohyb) přepínatelné bez ztráty energie. |
| Cleric | Priest → **Hierofant** | Útočná kouzla přeskakují na další cíle. |
| | Paladin → **Templář** | Aura léčí spojence; Svatá zbraň je trvalá. |
| Nekromant | Pán kostí → **Král kostí** | Služebníci se vyvinou do kostěných rytířů s vlastními schopnostmi. |
| | Žnec duší → **Posel smrti** | Nepřátelé pod 15 % HP jsou okamžitě popraveni. |

---

## Fáze 3 – Nový obsah dungeonu (rozšířeno 2026-10-17)

Stav: Fáze 1 (Nekromant) a 2 (evoluce podtříd) jsou hotové a commitnuté.

### 3a – Náhodní bossové v každé oblasti
Každá oblast dostane 2–3 bossy, ze kterých se podle seedu vybere jeden. Hráč tak dopředu neví, na koho narazí.
| Oblast | Původní | Nově navíc (návrhy) |
|---|---|---|
| Kanály (5) | Goo | **Matka krys**: přivolává krysy, noří se do vody, otravuje kousnutím |
| Vězení (10) | Tengu | Vrchní žalářník (řetězy, cely, stráže) |
| Jeskyně (15) | DM-300 | Krystalová královna (krystalové stěny, odrazy) |
| Město (20) | Král trpaslíků | Arcimág golemů |
| Hrob (25) | Yog-Dzewa | (později) |

Každý nový boss má **vlastní drop do alchymie** (např. Morová žláza → Morový lektvar).

### 3b – Nové stavy, lektvary a crafting
- Nové stavy (buffy/debuffy): **Mor** (šíří se na sousedy po smrti hostitele), Krvácení duše, Omrzliny, Posvěcení…
- Nové lektvary, brewy a elixíry z bossích dropů a nových surovin.
- Nové recepty v alchymii pro každý bossí drop.

### 3c – Volba cesty (větvení dungeonu)
Po 2. bossovi (a znovu po 4.) si hráč na schodech vybere ze 2 cest, například:
- **Ledová jeskyně** (zpomalující led, omrzliny, ledová monstra) vs. klasické Jeskyně
- **Spálené město** (oheň, láva) vs. klasické Město
Každá větev má vlastní tileset (zatím přebarvený), monstra, pasti a bossy.

### 3d – Nová monstra a předměty
- 2–4 nová monstra na oblast + elitní varianty.
- Nové běžné zbraně, zbroje, prsteny, hůlky a speciální předměty.

### 3e – Nová oblast 26–30 + nový finální boss (původní Fáze 3)
Bez změny oproti původnímu plánu, navazuje na ending (Fáze 4).

### Hexagony – posouzení
Celá hra je postavená na čtvercové mřížce: pohyb (8 směrů), hledání cest, výpočet viditelnosti (FOV),
dráhy střel, generátor místností (obdélníky), vykreslování dlaždic, automatické napojování zdí a veškerá
grafika prostředí. Přechod na hexy by znamenal přepsat jádro enginu a překreslit všechny tilesety.
Je to v podstatě nová hra a rozbilo by to všechen dosavadní obsah.
Navíc hex má jen 6 sousedů, kdežto SPD už teď umí 8 směrů (včetně diagonál).
**Doporučení:** hexy nedělat. Pokud jde o lepší pocit z pohybu, lze místo toho přidat např. úskok,
plynulejší animace nebo pohyb myší po cestě.

## Fáze 4 – Nový ending

- Po získání amuletu volba: **Utéct s amuletem** (klasický výstup nahoru, upravená Výzva vzestupu přes všech 30 pater) nebo **Zničit amulet** (nový finální souboj v jádru propasti).
- Dva různé epilogy + nové odznaky za každé zakončení a za každou postavu.

---

## Fáze 5 – Variabilita (aby se hry neopakovaly)

- **Události patra** (náhodně ~1 z 4 pater): Krvavý měsíc (silnější a rychlejší monstra, víc loot), Mlha, Zlatý skřet (utíká s pokladem), Prokleté patro, Klidné patro.
- **Alternativní bossové**: každá oblast náhodně jeden ze 2 bossů (nový boss pro kanály, vězení, jeskyně…).
- **Varianty monster** – elitní verze s modifikátory (ohnivý, rychlý, štítový, rozdvojující se).
- **Nové místnosti a mini‑úkoly** (oltář obětí, aréna, hazardní automat, zamčená knihovna).
- **Nové předměty**: 6–10 zbraní, 2–3 hůlky, 4–6 trinketů, nové recepty alchymie.
- **Modifikátory běhu** při startu (volitelné, obdoba Výzev, ale pozitivní i negativní).

---

## Fáze 6 – Balanc a ladění

- Testovací hry s každou třídou, ladění čísel, kontrola savů a pádů.

---

## Rizika a poznámky

- **Rozsah**: každá fáze je samostatně hratelná; doporučené pořadí 1 → 2 → 3 → 4 → 5.
- **Grafika** je největší ruční práce – dočasné přebarvené sprity se dají později vyměnit beze změny kódu.
- **Licence GPLv3**: úpravy je možné šířit, ale se zdrojovým kódem a pod stejnou licencí. Při vydání doporučuji jiný název hry.
- **Aktualizace originálu**: čím víc změn, tím těžší přebírat nové verze SPD od autora.

## Otevřené otázky

1. Jméno nové postavy – *Nekromant* je OK, nebo jiné (Hrobník, Vyvolávač duší)?
2. Odemčení nové postavy – podmínka, nebo rovnou odemčená?
3. Staré uložené hry – nepodporovat (doporučeno)?
