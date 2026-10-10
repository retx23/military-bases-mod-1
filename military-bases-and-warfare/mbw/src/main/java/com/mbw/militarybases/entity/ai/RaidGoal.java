package com.mbw.militarybases.entity.ai;

import com.mbw.militarybases.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.EnumSet;

/** Akın askerleri: hedef koordinata 24'er blokluk adımlarla ilerler (uzak mesafe pathfinding sorunu için). */
public class RaidGoal extends Goal {
    private final SoldierEntity mob;
    private int repath;

    public RaidGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() { return mob.raidTarget != null && mob.getTarget() == null; }
    @Override public boolean canContinueToUse() { return canUse(); }

    @Override public void tick() {
        BlockPos t = mob.raidTarget;
        if (t == null) return;
        double dx = t.getX() + 0.5 - mob.getX(), dz = t.getZ() + 0.5 - mob.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 22) { mob.raidTarget = null; return; }
        if (--repath > 0 && !mob.getNavigation().isDone()) return;
        repath = 40;
        double step = Math.min(24, d);
        int x = (int) Math.floor(mob.getX() + dx / d * step);
        int z = (int) Math.floor(mob.getZ() + dz / d * step);
        BlockPos probe = new BlockPos(x, mob.getBlockY(), z);
        if (!mob.level().hasChunkAt(probe)) return; // yüklü değilse bekle
        int y = mob.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        mob.getNavigation().moveTo(x + 0.5, y, z + 0.5, 1.15);
    }
}
