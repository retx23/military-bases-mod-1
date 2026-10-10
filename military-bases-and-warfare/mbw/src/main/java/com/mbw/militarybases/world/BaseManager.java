package com.mbw.militarybases.world;

import com.mbw.militarybases.Config;
import com.mbw.militarybases.Faction;
import com.mbw.militarybases.entity.SoldierEntity;
import com.mbw.militarybases.entity.TurretEntity;
import com.mbw.militarybases.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.items.ItemHandlerHelper;

/** Üslerin çalışma zamanı mantığı: doğurma, akın, ele geçirme ödülü, görevler. */
public final class BaseManager {
    private BaseManager() {}
    private static final String QUEST_KEY = "mbw_quest";

    // ---------------------------------------------------------------- doğurma (oyuncu yaklaşınca)
    public static void populate(ServerLevel level, BaseData data) {
        for (BaseData.Base b : data.bases.values()) {
            if (b.populated) continue;
            BlockPos c = b.center();
            if (level.getNearestPlayer(c.getX(), c.getY(), c.getZ(), 56, false) == null) continue;
            if (!level.hasChunksAt(b.origin, b.origin.offset(Layout.W - 1, 0, Layout.D - 1))) continue;
            populateBase(level, b);
            b.populated = true;
            data.setDirty();
        }
    }

    private static void populateBase(ServerLevel level, BaseData.Base b) {
        BlockPos o = b.origin, center = b.center();
        long key = b.key();
        int n = Math.min(Config.SOLDIERS_PER_BASE.get(), Layout.SOLDIERS.length);
        for (int i = 0; i < n; i++) {
            int[] p = Layout.SOLDIERS[i];
            int radius = (i % 3 == 0) ? 70 : 14;           // her 3. asker geniş devriye
            if (SoldierEntity.spawn(level, o.offset(p[0], 1, p[1]), b.faction, false, key, center, radius) != null) b.alive++;
        }
        if (SoldierEntity.spawn(level, o.offset(Layout.COMMANDER[0], 1, Layout.COMMANDER[1]), b.faction, true, key, center, 5) != null) b.alive++;

        if (Config.TURRETS_ENABLED.get()) {
            for (int[] p : Layout.CANNONS) TurretEntity.spawn(level, ModEntities.CANNON.get(), o.offset(p[0], 1, p[1]), b.faction, true);
            for (int[] p : Layout.MORTARS) TurretEntity.spawn(level, ModEntities.MORTAR.get(), o.offset(p[0], 1, p[1]), b.faction, true);
            for (int[] p : Layout.TANKS) TurretEntity.spawn(level, ModEntities.TANK.get(), o.offset(p[0], 1, p[1]), b.faction, true);
        }
    }

    // ---------------------------------------------------------------- akınlar
    public static void raids(ServerLevel level, BaseData data) {
        long now = level.getGameTime();
        long interval = Config.RAID_INTERVAL_SECONDS.get() * 20L;
        for (BaseData.Base b : data.bases.values()) {
            if (b.faction != Faction.RED || b.captured || !b.populated || b.alive <= 1) continue;
            if (now - b.lastRaid < interval) continue;
            BlockPos c = b.center();
            Player player = level.getNearestPlayer(c.getX(), c.getY(), c.getZ(), 220, p -> !p.isSpectator() && !p.isCreative());
            if (player == null) continue;
            b.lastRaid = now;
            data.setDirty();

            BlockPos target = player.blockPosition();
            boolean vsBlue = false;
            BaseData.Base blue = data.nearest(c, Faction.BLUE, 260, true);
            if (blue != null && level.random.nextBoolean()) { target = blue.center(); vsBlue = true; }

            BlockPos gate = b.origin.offset(50, 0, -4);
            if (!level.isLoaded(gate)) continue;
            for (int i = 0; i < Config.RAID_SIZE.get(); i++) {
                BlockPos sp = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, gate.offset(i % 3 - 1, 0, i / 3));
                SoldierEntity s = SoldierEntity.spawn(level, sp, Faction.RED, false, null, null, 0);
                if (s != null) s.raidTarget = target;
            }
            player.sendSystemMessage(Component.translatable(vsBlue ? "msg.mbw.raid_blue" : "msg.mbw.raid_player"));
        }
    }

    // ---------------------------------------------------------------- ölüm / ele geçirme
    public static void onBaseUnitDied(ServerLevel level, long key) {
        BaseData data = BaseData.get(level);
        BaseData.Base b = data.bases.get(key);
        if (b == null) return;
        b.alive = Math.max(0, b.alive - 1);
        data.setDirty();
        if (b.alive == 0 && !b.captured && b.faction == Faction.RED) capture(level, data, b);
    }

    private static void capture(ServerLevel level, BaseData data, BaseData.Base b) {
        b.captured = true;
        data.setDirty();
        // bayrağı maviye çevir
        BlockPos o = b.origin;
        BlockPos from = o.offset(Layout.FLAG_X + 1, Layout.FLAG_H - 3, Layout.FLAG_Z);
        BlockPos to = o.offset(Layout.FLAG_X + 6, Layout.FLAG_H, Layout.FLAG_Z);
        if (level.isLoaded(from) && level.isLoaded(to))
            for (BlockPos p : BlockPos.betweenClosed(from, to)) level.setBlock(p, BaseBuilder.flagBlock(Faction.BLUE).defaultBlockState(), 3);

        BlockPos c = b.center();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(c.getX(), c.getY(), c.getZ()) > 160.0 * 160.0) continue;
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Items.DIAMOND, 3 + level.random.nextInt(4)));
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Items.EMERALD, 10 + level.random.nextInt(10)));
            ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Items.GOLDEN_APPLE, 2));
            p.giveExperiencePoints(300);
            p.sendSystemMessage(Component.translatable("msg.mbw.captured"));

            CompoundTag pers = persisted(p);
            if (pers.contains(QUEST_KEY) && pers.getLong(QUEST_KEY) == b.key()) {
                pers.remove(QUEST_KEY);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Items.NETHERITE_SCRAP, 2));
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Items.EMERALD, 24));
                p.giveExperiencePoints(500);
                p.sendSystemMessage(Component.translatable("msg.mbw.quest_done"));
            }
        }
    }

    // ---------------------------------------------------------------- görevler
    private static CompoundTag persisted(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void questInteract(ServerPlayer p, ServerLevel level) {
        BaseData data = BaseData.get(level);
        CompoundTag pers = persisted(p);

        if (pers.contains(QUEST_KEY)) {
            BaseData.Base q = data.bases.get(pers.getLong(QUEST_KEY));
            if (q != null && !q.captured) {
                BlockPos c = q.center();
                p.sendSystemMessage(Component.translatable("msg.mbw.quest_active", c.getX(), c.getZ()));
                return;
            }
            pers.remove(QUEST_KEY);
        }
        BaseData.Base best = data.nearest(p.blockPosition(), Faction.RED, 100000, true);
        if (best == null) {
            p.sendSystemMessage(Component.translatable("msg.mbw.quest_none"));
            return;
        }
        pers.putLong(QUEST_KEY, best.key());
        BlockPos c = best.center();
        int dist = (int) Math.sqrt(c.distSqr(p.blockPosition()));
        p.sendSystemMessage(Component.translatable("msg.mbw.quest_new", c.getX(), c.getZ(), dist));
    }
}
