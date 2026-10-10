package com.mbw.militarybases.entity.ai;

import com.mbw.militarybases.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Taktiksel savaş davranışı:
 *  - hedefi görmüyorsa ilerler (flank)
 *  - görüyorsa mesafe ayarlar (çok yakınsa geri çekilir, uzaksa yaklaşır), yanlara kayar
 *  - nişan alır (gecikmeli), seri atış yapar
 *  - şarjör bitince siper alıp şarjör değiştirir
 *  - belirli aralıklarla "peek" yapıp siper arkasına saklanır, canı azsa da siper alır
 */
public class SoldierCombatGoal extends Goal {
    private final SoldierEntity mob;
    private int coverTicks, burstLeft, burstPause, repath, nextPeek = 80, aimTicks, strafeTicks;
    private float strafeDir = 0.6f;
    private BlockPos cover;

    public SoldierCombatGoal(SoldierEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override public boolean canUse() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() {
        nextPeek = 60 + mob.getRandom().nextInt(80);
        aimTicks = 0; burstLeft = 0; burstPause = 0; coverTicks = 0; cover = null;
    }

    @Override public void stop() {
        mob.setAiming(false);
        mob.getNavigation().stop();
        cover = null; coverTicks = 0; aimTicks = 0;
    }

    @Override public void tick() {
        LivingEntity t = mob.getTarget();
        if (t == null) return;
        double dist = mob.distanceTo(t);
        mob.getLookControl().setLookAt(t, 40f, 40f);

        // --- SİPER / ŞARJÖR ---
        if (coverTicks > 0 || mob.isReloading()) {
            if (coverTicks <= 0) coverTicks = 1;
            coverTicks--;
            mob.setAiming(false);
            aimTicks = 0;
            if (cover == null) cover = findCover(t);
            if (cover != null && mob.blockPosition().distSqr(cover) > 2.0) {
                if (--repath <= 0) {
                    mob.getNavigation().moveTo(cover.getX() + 0.5, cover.getY(), cover.getZ() + 0.5, 1.3);
                    repath = 10;
                }
            } else {
                mob.getNavigation().stop();
            }
            if (coverTicks <= 0) cover = null;
            return;
        }

        boolean see = mob.getSensing().hasLineOfSight(t);

        // canı azsa ara sıra siper al
        if (mob.getHealth() < mob.getMaxHealth() * 0.4f && mob.getRandom().nextInt(100) == 0) {
            coverTicks = 60; return;
        }
        // periyodik "peek": kısa süre saklan, sonra tekrar çık
        if (--nextPeek <= 0 && see && dist < 40) {
            coverTicks = 25 + mob.getRandom().nextInt(25);
            nextPeek = 100 + mob.getRandom().nextInt(100);
            cover = null;
            return;
        }

        // --- HEDEFİ GÖRMÜYOR: ilerle ---
        if (!see) {
            mob.setAiming(false);
            aimTicks = 0;
            if (--repath <= 0) { mob.getNavigation().moveTo(t, 1.15); repath = 10; }
            return;
        }

        // --- HEDEFİ GÖRÜYOR: mesafe ayarla ---
        if (dist > 32) {
            if (--repath <= 0) { mob.getNavigation().moveTo(t, 1.0); repath = 10; }
        } else if (dist < 9) {
            if (--repath <= 0) {
                Vec3 away = DefaultRandomPos.getPosAway(mob, 8, 4, t.position());
                if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
                repath = 10;
            }
        } else {
            mob.getNavigation().stop();
            if (--strafeTicks <= 0) {
                strafeTicks = 20 + mob.getRandom().nextInt(30);
                strafeDir = mob.getRandom().nextBoolean() ? 0.6f : -0.6f;
            }
            mob.getMoveControl().strafe(0.0f, strafeDir * 0.5f);
        }

        // --- NİŞAN & ATEŞ ---
        mob.setAiming(true);
        if (aimTicks < 8) { aimTicks++; return; }
        if (mob.getShootCooldown() > 0) return;

        if (burstLeft > 0) {
            mob.fireAt(t);
            burstLeft--;
            if (mob.getAmmo() <= 0) {
                mob.startReload();
                burstLeft = 0;
            } else {
                mob.setShootCooldown(mob.getLoadout().interval());
            }
            if (burstLeft == 0) burstPause = 10 + mob.getRandom().nextInt(15);
        } else if (--burstPause <= 0) {
            burstLeft = mob.getLoadout().burst() + mob.getRandom().nextInt(3);
        }
    }

    /** Tehdidin görüş hattını kesen, yakındaki en yakın noktayı bulur. */
    private BlockPos findCover(LivingEntity threat) {
        BlockPos me = mob.blockPosition();
        Vec3 eye = threat.getEyePosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < 14; i++) {
            BlockPos p = me.offset(mob.getRandom().nextInt(19) - 9, mob.getRandom().nextInt(5) - 2, mob.getRandom().nextInt(19) - 9);
            if (!mob.level().getBlockState(p).isAir() || !mob.level().getBlockState(p.above()).isAir()) continue;
            if (!mob.level().getBlockState(p.below()).isSolid()) continue;
            Vec3 head = Vec3.atBottomCenterOf(p).add(0, 1.6, 0);
            HitResult r = mob.level().clip(new ClipContext(eye, head, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, threat));
            if (r.getType() == HitResult.Type.MISS) continue; // hâlâ görünüyor
            double d = p.distSqr(me);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }
}
