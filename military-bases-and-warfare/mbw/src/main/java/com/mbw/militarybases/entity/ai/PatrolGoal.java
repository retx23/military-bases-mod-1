package com.mbw.militarybases.entity.ai;

import com.mbw.militarybases.entity.SoldierEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Üssün (home) etrafında devriye gezer. Yarıçap dışına çıkarsa eve doğru çekilir. */
public class PatrolGoal extends Goal {
    private final SoldierEntity mob;
    private Vec3 dest;

    public PatrolGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override public boolean canUse() {
        if (!mob.hasHome || mob.getTarget() != null || mob.raidTarget != null || mob.isReloading()) return false;
        if (mob.getRandom().nextInt(80) != 0) return false;
        double r = mob.patrolRadius;
        Vec3 home = Vec3.atBottomCenterOf(mob.home);
        dest = mob.blockPosition().distSqr(mob.home) > r * r
                ? DefaultRandomPos.getPosTowards(mob, 14, 6, home, Math.PI / 2)
                : DefaultRandomPos.getPos(mob, 14, 6);
        return dest != null;
    }

    @Override public void start() { mob.getNavigation().moveTo(dest.x, dest.y, dest.z, 0.85); }
    @Override public boolean canContinueToUse() { return !mob.getNavigation().isDone() && mob.getTarget() == null; }
    @Override public void stop() { mob.getNavigation().stop(); }
}
