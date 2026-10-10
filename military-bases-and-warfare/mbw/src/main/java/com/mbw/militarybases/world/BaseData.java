package com.mbw.militarybases.world;

import com.mbw.militarybases.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Dünyadaki tüm üslerin kaydı (kalıcı). Yapı üretimi worker thread'de çalıştığı için
 * üsler önce PENDING kuyruğuna eklenir, ana thread'de buraya alınır.
 */
public class BaseData extends SavedData {
    public record Pending(BlockPos origin, Faction faction) {}
    public static final Queue<Pending> PENDING = new ConcurrentLinkedQueue<>();

    public static class Base {
        public final BlockPos origin;   // y = zemin blok seviyesi
        public final Faction faction;
        public boolean populated, captured;
        public int alive;
        public long lastRaid;

        public Base(BlockPos origin, Faction faction) { this.origin = origin; this.faction = faction; }
        public BlockPos center() { return origin.offset(Layout.CX, 0, Layout.CZ); }
        public long key() { return origin.asLong(); }
    }

    public final Map<Long, Base> bases = new HashMap<>();

    public static BaseData get(ServerLevel any) {
        return any.getServer().overworld().getDataStorage().computeIfAbsent(BaseData::load, BaseData::new, "mbw_bases");
    }

    public void drainPending(ServerLevel level) {
        Pending p;
        while ((p = PENDING.poll()) != null) {
            final Pending pp = p;
            bases.computeIfAbsent(pp.origin().asLong(), k -> {
                Base b = new Base(pp.origin(), pp.faction());
                b.lastRaid = level.getGameTime();
                return b;
            });
            setDirty();
        }
    }

    public Base nearest(BlockPos from, Faction f, double maxDist, boolean aliveOnly) {
        Base best = null;
        double bd = maxDist * maxDist;
        for (Base b : bases.values()) {
            if (b.faction != f) continue;
            if (aliveOnly && (b.captured || (b.populated && b.alive <= 0))) continue;
            double d = b.center().distSqr(from);
            if (d < bd) { bd = d; best = b; }
        }
        return best;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Base b : bases.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("Origin", b.origin.asLong());
            t.putInt("Faction", b.faction.ordinal());
            t.putBoolean("Populated", b.populated);
            t.putBoolean("Captured", b.captured);
            t.putInt("Alive", b.alive);
            t.putLong("LastRaid", b.lastRaid);
            list.add(t);
        }
        tag.put("Bases", list);
        return tag;
    }

    public static BaseData load(CompoundTag tag) {
        BaseData d = new BaseData();
        for (Tag raw : tag.getList("Bases", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) raw;
            Base b = new Base(BlockPos.of(t.getLong("Origin")), Faction.byId(t.getInt("Faction")));
            b.populated = t.getBoolean("Populated");
            b.captured = t.getBoolean("Captured");
            b.alive = t.getInt("Alive");
            b.lastRaid = t.getLong("LastRaid");
            d.bases.put(b.key(), b);
        }
        return d;
    }
}
