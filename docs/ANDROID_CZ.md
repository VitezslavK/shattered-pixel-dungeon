# SPD Mod na Androidu: jak ho nainstalovat

Hra není v Obchodě Play, instaluje se ze souboru `SPD-mod.apk`. Android takové
aplikace hlídá víc, proto je potřeba pár potvrzení navíc. Na hru to nemá vliv.

## 1. Stažení

Nejspolehlivější je **odkaz na Google Disk** (ne poslat soubor přímo do chatu):

- Ten, kdo hru rozesílá, nahraje `SPD-mod.apk` na Google Disk,
  dá **Sdílet → Kdokoli s odkazem** a pošle odkaz.
- Na telefonu otevři odkaz v **Chrome** a dej **Stáhnout**.
  - Disk napíše, že soubor nemůže zkontrolovat na viry: dej **Přesto stáhnout**.
  - Chrome může napsat „Soubor může být škodlivý“: dej **Přesto stáhnout**.

Proč ne přes chat: Messenger, Instagram a Discord soubory `.apk` často zablokují nebo
přejmenují. WhatsApp obvykle funguje, ale pošli ho jako **Dokument**, ne jako fotku.

## 2. Instalace

1. Otevři stažený soubor (oznámení o stažení, nebo aplikace **Soubory → Stažené**).
2. Android napíše, že instalace z tohoto zdroje není povolená:
   dej **Nastavení** a zapni **Povolit z tohoto zdroje** (pro Chrome nebo Soubory).
   Pak se vrať zpět.
3. Dej **Instalovat**.
4. Pokud vyskočí **Google Play Protect**:
   - „Odeslat aplikaci ke kontrole?“: dej **Neodesílat** nebo **Instalovat bez kontroly**.
   - „Nebezpečná aplikace zablokována“ / „Neznámá aplikace“: ťukni na **Další podrobnosti**
     (malá šipka) a dej **Přesto nainstalovat**.

Hra se objeví jako **SPD Mod** vedle původní hry, nic nepřepisuje.

## Když Play Protect instalaci úplně zakáže

Na některých telefonech je jen tlačítko OK. Pak:

1. Otevři **Obchod Play → ikona profilu vpravo nahoře → Play Protect → ozubené kolečko**.
2. Dočasně vypni **Kontrolovat aplikace pomocí Play Protect**.
3. Nainstaluj hru.
4. Kontrolu hned **zase zapni**.

## Aktualizace

Novější `SPD-mod.apk` se instaluje stejně a přepíše starou verzi, uložené hry zůstanou.
Pro online hraní musí mít všichni stejnou verzi.

Pokud jsi měl nainstalovanou starší verzi s názvem „Shattered Pixel Dungeon Mod“,
odinstaluj ji: nová verze je samostatná aplikace „SPD Mod“.

## Pro vývojáře

Release APK se podepisuje vlastním klíčem `~/.android/spdmod-release.jks` (heslo
v `~/.android/spdmod-release.properties`). Klíč není v repozitáři. **Zálohuj si ho**:
bez něj nepůjde vydat aktualizaci, která se nainstaluje přes starou verzi.

```
./gradlew android:assembleRelease
cp ~/.spd-build/android/outputs/apk/release/android-release.apk dist/SPD-mod.apk
```
