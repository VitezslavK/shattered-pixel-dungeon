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

## Fáze 3 – Delší dungeon (nová oblast 26–30)

Dnes: 5 oblastí × 5 pater, Yendorův amulet na 26. patře.

Nově: po Yog-Dzewě (25) **schod dolů místo amuletu** → nová 6. oblast **Rozervaná propast** (patra 26–29) + **nový boss na 30. patře**, amulet na 31.

- 4–5 nových monster (např. prázdnotní chodec, požírač světla, zrcadlová stvůra, rozpadlý anděl) + 1 vzácná varianta.
- Nové pastě, nový typ terénu (trhliny do prázdna – pád = poškození a přesun o patro níž).
- Obchod na 26. patře, škálování předmětů: nová 6. úroveň zbraní a zbrojí (2–3 zbraně, 1 zbroj).
- Úprava všech míst, kde je natvrdo „25/26“ (generátor pater, spawny, obchody, odznaky, Výzva vzestupu).

**Boss 30. patra – „Strážce Amuletu“** (návrh): vícefázový souboj, který mění arénu (propadající se podlaha, fáze zrcadlení hrdinova vybavení).

---

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
