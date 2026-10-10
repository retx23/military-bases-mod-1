package com.mbw.militarybases;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder B = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue SOLDIERS_PER_BASE =
            B.comment("Her üste doğan asker sayısı (düşük sistem için azalt). Max 14.")
             .defineInRange("soldiersPerBase", 12, 0, 14);
    public static final ForgeConfigSpec.BooleanValue TURRETS_ENABLED =
            B.comment("Top / havan / tank doğsun mu?").define("turretsEnabled", true);
    public static final ForgeConfigSpec.BooleanValue RAIDS_ENABLED =
            B.comment("Kırmızı üsler akın düzenlesin mi?").define("raidsEnabled", true);
    public static final ForgeConfigSpec.IntValue RAID_INTERVAL_SECONDS =
            B.comment("Bir kırmızı üssün iki akını arasındaki süre (saniye).")
             .defineInRange("raidIntervalSeconds", 900, 60, 86400);
    public static final ForgeConfigSpec.IntValue RAID_SIZE =
            B.comment("Bir akındaki asker sayısı.").defineInRange("raidSize", 5, 1, 12);
    public static final ForgeConfigSpec.BooleanValue BLOCK_DAMAGE =
            B.comment("Top mermileri blokları yıksın mı? (false = sadece can hasarı)")
             .define("artilleryBlockDamage", true);
    public static final ForgeConfigSpec.DoubleValue SOLDIER_SPREAD =
            B.comment("Askerlerin nişan sapması (derece). Düşük = daha isabetli.")
             .defineInRange("soldierSpreadDegrees", 2.5, 0.0, 15.0);

    public static final ForgeConfigSpec SPEC = B.build();
}
