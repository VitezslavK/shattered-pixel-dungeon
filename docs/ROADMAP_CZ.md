# Roadmapa úprav Shattered Pixel Dungeon

Základ: SPD v3.3.8 (Java + libGDX). Cíl: delší dungeon, nová postava, evoluce podtříd, nový ending a víc náhody, aby se hry tolik neopakovaly.

Pořadí fází je zvolené tak, aby každá fáze šla sestavit a hrát sama o sobě.

---

## Stav k 2026-10-17: všechny fáze implementovány

Hotovo a commitnuto na větvi `mod-overhaul`: Nekromant, evoluce podtříd, alternativní bossové (patra 5–25),
větvení (Ledové jeskyně, Spálené město), oblast Propast 26–30 s Hlasatelem prázdnoty, 5 konců + odznaky,
Arkanista, Alchymista, reakce, runy a Kodex, nová monstra a elitní nepřátelé, nové zbraně/hůlka/prsten/očarování/glyf,
události pater, svatyně, skřet s pokladem, modifikátory běhu, online duchové, žebříček denních běhů a sdílené kosti.

Zbývá: ladění podle hraní (balanc), vlastní grafika místo přebarvených placeholderů, jazyky kromě EN/CS.

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

## Fáze 4 – Více endingů (rozšířeno 2026-10-17)

Konec nebude vždy stejný. Ending určí rozhodnutí a činy během hry:
| Ending | Podmínka | Výsledek |
|---|---|---|
| **Útěk s amuletem** (klasický) | vynést amulet na povrch | současný konec |
| **Zničení amuletu** | rozbít amulet v jádru propasti, souboj s jeho strážcem | dungeon se zhroutí, útěk na čas |
| **Nový vládce** | nasadit si amulet (temná volba, hlavně Lich / Posel smrti) | hrdina se stane novým Yogem, temný epilog |
| **Osvoboditel** | splnit všechny úkoly NPC a ušetřit Krysího krále | NPC pomohou při útěku, „dobrý“ epilog |
| **Tajný ending** | získat všech 5 bossích surovin jednoho běhu a vyrobit z nich Klíč hlubin | otevře skrytou oblast a finálního tajného bosse |

Každý ending má vlastní obrazovku, text, odznak a záznam v žebříčku.

## Fáze 5 – Variabilita (aby se hry neopakovaly)

- **Události patra** (náhodně ~1 z 4 pater): Krvavý měsíc (silnější a rychlejší monstra, víc loot), Mlha, Zlatý skřet (utíká s pokladem), Prokleté patro, Klidné patro.
- **Alternativní bossové**: každá oblast náhodně jeden ze 2 bossů (nový boss pro kanály, vězení, jeskyně…).
- **Varianty monster** – elitní verze s modifikátory (ohnivý, rychlý, štítový, rozdvojující se).
- **Nové místnosti a mini‑úkoly** (oltář obětí, aréna, hazardní automat, zamčená knihovna).
- **Nové předměty**: 6–10 zbraní, 2–3 hůlky, 4–6 trinketů, nové recepty alchymie.
- **Modifikátory běhu** při startu (volitelné, obdoba Výzev, ale pozitivní i negativní).

---

## Fáze 5b – Nové třídy (schváleno 2026-10-17)

### Arkanista – mistr magických předmětů
- Start: **Arkanická rukavice** (třídní předmět) + 2 sloty na artefakty místo 1.
- Mechanika: rozkládá předměty na **arkanovou esenci**, kterou nabíjí a vylepšuje artefakty, prsteny a hůlky.
- Podtřídy: **Runář** (vpisuje runy do zbraní a zbrojí, kombinuje očarování) / **Strážce relikvií** (artefakty se nabíjejí rychleji a lze je přebít nad maximum).
- Schopnosti zbroje: Arkánový výboj, Přetížení artefaktu, Zrcadlová bariéra.

### Alchymista – mistr lektvarů a craftingu
- Start: **Polní kotlík** (vaří lektvary kdekoliv, bez alchymistického stolu) + 2 bomby.
- Mechanika: hozené lektvary mají větší dosah a silnější efekt; z bossích surovin vyrobí víc.
- Podtřídy: **Bombardér** (výbušniny, řetězové reakce) / **Transmutátor** (mění předměty a terén, kámen na zlato).
- Schopnosti zbroje: Kyselý déšť, Elixír berserka, Transmutace.

## Fáze 5c – Postupně objevované mechaniky (nové)
- **Elementární reakce**: kombinace stavů vytváří reakce (mokrý + elektřina = šok na celou kaluž, hořící + olej = exploze, zmrzlý + drtivý úder = roztříštění, mor + oheň = spálení nákazy). Hra je nevysvětluje předem. Když reakci poprvé způsobíš, zapíše se do **Kodexu objevů**.
- **Runové nápisy**: na zdech jsou staré runy. Každá rozluštěná runa se trvale (napříč hrami) zapíše do Kodexu a odemkne nové recepty, tajné místnosti a skryté vlastnosti předmětů.
- **Kodex objevů**: nová záložka deníku. Ukazuje počet objevených mechanik a nápovědy k neobjeveným (např. „??? + oheň“).
- **Svatyně a oltáře**: požehnání výměnou za oběť (HP, předmět, zlato), každá svatyně má jinou cenu.

## Fáze 5d – Asynchronní online multiplayer (schváleno 2026-10-17)
Hra zůstane tahová a pro jednoho hráče, ostatní hráči do ní vstupují asynchronně přes malý server:
- **PvP – Invaze duchů**: po vítězství nebo smrti se tvůj hrdina (třída, výbava, talenty) nahraje jako „duch“. Ostatním hráčům se může objevit jako nepřátelský miniboss řízený AI. Kdo ho porazí, získá trofej a část jeho výbavy.
- **Co-op – Přivolání spojence**: jednou za běh lze přivolat ducha hrdiny kamaráda (podle kódu přítele) jako spojence řízeného AI.
- **Závody na seedu**: týdenní seed, všichni hrají stejný dungeon, společný žebříček (čas, skóre, hloubka).
- **Sdílené kosti**: ostatky ostatních hráčů se mohou objevit ve tvém běhu.
- Technika: malý backend (např. Supabase zdarma, REST + JSON snapshot hrdiny). Účet založí vlastník projektu, klíč se nebude ukládat do repozitáře.

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
