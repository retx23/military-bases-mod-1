package com.mbw.militarybases;

import net.minecraft.util.RandomSource;

/**
 * Askerin TaCZ silah seti. gunId'ler TaCZ varsayılan paketindeki kimliklerdir;
 * farklı bir silah paketi kullanıyorsan buradan değiştir.
 */
public record Loadout(String gunId, String fireMode, int mag, int interval, int burst, float damage, int weight) {

    public static final Loadout[] POOL = {
            new Loadout("tacz:ak47",       "AUTO", 30, 3, 4, 4.5f, 40),
            new Loadout("tacz:m4a1",       "AUTO", 30, 3, 4, 4.0f, 30),
            new Loadout("tacz:hk_mp5a5",   "AUTO", 30, 2, 5, 3.0f, 15),
            new Loadout("tacz:glock_17",   "SEMI", 17, 6, 2, 3.0f, 10),
            new Loadout("tacz:m700",       "SEMI",  5, 30, 1, 9.0f, 5),
    };

    public static int pick(RandomSource r) {
        int total = 0;
        for (Loadout l : POOL) total += l.weight;
        int roll = r.nextInt(total);
        for (int i = 0; i < POOL.length; i++) {
            roll -= POOL[i].weight;
            if (roll < 0) return i;
        }
        return 0;
    }

    public static Loadout get(int idx) {
        return POOL[Math.floorMod(idx, POOL.length)];
    }
}
