package com.mbw.militarybases.compat;

import com.mbw.militarybases.Loadout;
import com.mbw.militarybases.MilitaryBasesMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * TaCZ köprüsü. Derleme zamanında TaCZ'e bağımlı DEĞİL; reflection kullanır.
 * Böylece TaCZ sürümü değişip API kayarsa mod çökmez: asker "yedek" hitscan ateşine geçer.
 */
public final class TaczCompat {
    private static boolean init;
    private static Method FROM, SHOOT, DRAW, RELOAD, AIM;

    private TaczCompat() {}

    private static synchronized void init() {
        if (init) return;
        init = true;
        try {
            Class<?> c = Class.forName("com.tacz.guns.api.entity.IGunOperator");
            FROM = c.getMethod("fromLivingEntity", LivingEntity.class);
            for (Method m : c.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                switch (m.getName()) {
                    case "shoot" -> { if (p.length >= 2 && Supplier.class.isAssignableFrom(p[0])) SHOOT = m; }
                    case "draw" -> { if (p.length == 1 && Supplier.class.isAssignableFrom(p[0])) DRAW = m; }
                    case "reload" -> { if (p.length == 0) RELOAD = m; }
                    case "aim" -> { if (p.length == 1 && p[0] == boolean.class) AIM = m; }
                    default -> {}
                }
            }
            MilitaryBasesMod.LOGGER.info("[MBW] TaCZ IGunOperator bulundu (shoot={}, draw={}, reload={}, aim={})",
                    SHOOT != null, DRAW != null, RELOAD != null, AIM != null);
        } catch (Throwable t) {
            FROM = null;
            MilitaryBasesMod.LOGGER.warn("[MBW] TaCZ API bulunamadı, yedek ateş sistemi kullanılacak: {}", t.toString());
        }
    }

    /** TaCZ silah item'ını NBT ile üretir. TaCZ yoksa boş döner. */
    public static ItemStack createGun(Loadout l) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("tacz", "modern_kinetic_gun"));
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(item);
        CompoundTag t = s.getOrCreateTag();
        t.putString("GunId", l.gunId());
        t.putString("GunFireMode", l.fireMode());
        t.putInt("GunCurrentAmmoCount", l.mag());
        t.putBoolean("HasBulletInBarrel", true);
        return s;
    }

    /** Sonsuz cephane: şarjörü NBT'den doldurur. */
    public static void refill(ItemStack gun, int mag) {
        if (gun.isEmpty() || !gun.hasTag()) return;
        gun.getTag().putInt("GunCurrentAmmoCount", mag);
        gun.getTag().putBoolean("HasBulletInBarrel", true);
    }

    private static Object operator(LivingEntity e) throws Exception {
        return FROM.invoke(null, e);
    }

    public static void draw(LivingEntity e) {
        init();
        if (FROM == null || DRAW == null) return;
        try {
            Supplier<ItemStack> sup = e::getMainHandItem;
            DRAW.invoke(operator(e), sup);
        } catch (Throwable ignored) {}
    }

    public static void aim(LivingEntity e, boolean aim) {
        init();
        if (FROM == null || AIM == null) return;
        try { AIM.invoke(operator(e), aim); } catch (Throwable ignored) {}
    }

    public static void reload(LivingEntity e) {
        init();
        if (FROM == null || RELOAD == null) return;
        try { RELOAD.invoke(operator(e)); } catch (Throwable ignored) {}
    }

    /** @return ShootResult adı ("SUCCESS", "COOL_DOWN", ...) ya da TaCZ kullanılamıyorsa null. */
    public static String shoot(LivingEntity e, float pitch, float yaw) {
        init();
        if (FROM == null || SHOOT == null) return null;
        try {
            Supplier<Float> p = () -> pitch;
            Supplier<Float> y = () -> yaw;
            Object op = operator(e);
            Object r = SHOOT.getParameterCount() == 2
                    ? SHOOT.invoke(op, p, y)
                    : SHOOT.invoke(op, p, y, System.currentTimeMillis());
            return r == null ? null : r.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
