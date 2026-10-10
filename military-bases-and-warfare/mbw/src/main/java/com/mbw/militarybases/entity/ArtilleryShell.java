package com.mbw.militarybases.entity;

import com.mbw.militarybases.Config;
import com.mbw.militarybases.Faction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;

/** Top / havan / tank mermisi. Kendi yerçekimiyle uçar, çarpınca patlar. */
public class ArtilleryShell extends Projectile {
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(ArtilleryShell.class, EntityDataSerializers.FLOAT);
    private float power = 2.5f;

    public ArtilleryShell(EntityType<? extends ArtilleryShell> type, Level level) { super(type, level); }

    public void setPower(float p) { this.power = p; }
    public void setGravity(float g) { entityData.set(GRAVITY, g); }

    @Override protected void defineSynchedData() { entityData.define(GRAVITY, 0.03f); }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target)) return false;
        // aynı fraksiyonun askerlerinin / toplarının üstünde patlamasın
        Entity owner = getOwner();
        Faction mine = owner == null ? null : Faction.of(owner);
        return mine == null || Faction.of(target) != mine;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 mv = getDeltaMovement();
        if (!level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                return;
            }
            if (tickCount > 600 || getY() < level().getMinBuildHeight() - 32) { discard(); return; }
        } else {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.01, 0);
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0, 0, 0);
        }
        setPos(getX() + mv.x, getY() + mv.y, getZ() + mv.z);
        setDeltaMovement(mv.x, mv.y - entityData.get(GRAVITY), mv.z);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            Level.ExplosionInteraction mode = Config.BLOCK_DAMAGE.get() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE;
            level().explode(getOwner(), getX(), getY(), getZ(), power, mode);
            discard();
        }
    }

    @Override protected void addAdditionalSaveData(CompoundTag t) { t.putFloat("Power", power); t.putFloat("Grav", entityData.get(GRAVITY)); }
    @Override protected void readAdditionalSaveData(CompoundTag t) { power = t.getFloat("Power"); entityData.set(GRAVITY, t.getFloat("Grav")); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
