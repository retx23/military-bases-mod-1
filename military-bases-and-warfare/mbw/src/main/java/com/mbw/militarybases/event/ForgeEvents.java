package com.mbw.militarybases.event;

import com.mbw.militarybases.Config;
import com.mbw.militarybases.world.BaseData;
import com.mbw.militarybases.world.BaseManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEvents {

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        long t = level.getGameTime();
        if (t % 20 != 0) return;                       // saniyede 1 kez: düşük sistem dostu

        BaseData data = BaseData.get(level);
        data.drainPending(level);
        if (t % 40 == 0) BaseManager.populate(level, data);
        if (t % 200 == 0 && Config.RAIDS_ENABLED.get()) BaseManager.raids(level, data);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) {
        BaseData.PENDING.clear();
    }
}
