# Military Bases & Warfare (Forge 1.20.1)

Bağımlılıklar: Forge 47.x, GeckoLib 4.4.x, Timeless and Classics Zero (TaCZ).

## Kurulum
1. Resmi Forge 1.20.1 MDK'yı indir (files.minecraftforge.net) ve aç.
2. MDK içinden `gradlew`, `gradlew.bat` ve `gradle/` klasörünü bu proje köküne kopyala (wrapper için).
3. JDK 17 kur. Proje kökünde: `./gradlew build` -> `build/libs/militarybases-1.20.1-1.0.0.jar`
4. Jar'ı, GeckoLib ve TaCZ jar'larıyla birlikte `mods/` klasörüne koy.
5. Geliştirme testi: TaCZ jar'ını `run/mods/` içine koy, sonra `./gradlew runClient`.
   (IntelliJ: `./gradlew genIntellijRuns`, sonra projeyi Gradle olarak aç.)

## Test komutları
- `/locate structure mbw:red_base` ve `/locate structure mbw:blue_base`
- `/place structure mbw:red_base ~ ~ ~` (kendi önüne üs kurar; askerler yaklaşınca doğar)
- Yaratıcı sekmesi: "Military Bases & Warfare" (asker, komutan, top, havan, tank)
- Top/havan/tank: SHIFT + sağ tık = baktığın noktaya manuel ateş
- Mavi komutana sağ tık = görev (kırmızı üssü yok et)

## Ayarlar
`config/mbw-common.toml`: asker sayısı, akın aralığı/boyutu, top blok hasarı, nişan sapması.
Silah listesi: `Loadout.java` (TaCZ gun id'leri).

## Dosya haritası
- world/BaseStructure, BasePiece, BaseBuilder, Layout, Palette: üs üretimi (biyom paletli)
- world/BaseData, BaseManager: üs kaydı, doğurma, akın, ele geçirme ödülü, görev
- entity/SoldierEntity + ai/*: asker ve yapay zeka (siper, nişan, şarjör, devriye, akın)
- entity/TurretEntity, ArtilleryShell: top, havan, tank ve mermi
- compat/TaczCompat: TaCZ köprüsü (reflection)
- client/*: GeckoLib asker modeli ve tur renderer'ı
