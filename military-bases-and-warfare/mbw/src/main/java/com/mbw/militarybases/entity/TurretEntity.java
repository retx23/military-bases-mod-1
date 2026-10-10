package com.mbw.militarybases.entity;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Top, havan topu ve tank için ortak sınıf (tür = Kind).
 *  - Otomatik: menzildeki düşmanı bulur, taretini çevirir, ateş eder.
 *  - Manuel: SHIFT + sağ tık => baktığın noktaya ateş eder.
 *  - Tank: üssün çevresinde yavaşça devriye gezer, hedef görünce durup ateş eder.
 */
public class TurretEntity extends PathfinderMob {
    public enum Kind {
        //      menzil, minMenzil, bekleme(tick), güç, hız,  yerçekimi, havan(lob)
        CANNON(72,  8,  70, 2.4f, 3.0, 0.03, false),
        MORTAR(100, 16, 130, 3.0f, 0.0, 0.05, true),
        TANK(80,   10,  90, 2.0f, 3.2, 0.03, false);

        public final int range, minRange, cooldown;
        public final float power;
        public final double speed, gravity;
        public final boolean lob;

        Kind(int range, int minRange, int cooldown, float power, double speed, double gravity, boolean lob) {
            this.range = range; this.minRange = minRange; this.cooldown = cooldown;
            this.power = power; this.speed = speed; this.gravity = gravity; this.lob = lob;
        }
    }

    private static final EntityDataAccessor<Integer> FACTION = SynchedEntityData.defineId(TurretEntity.class, EntityDataSerializers.INT);
    private static final double LOB_ANGLE = Math.toRadians(62);

    public final Kind kind;
    private int cooldown = 40;
    public BlockPos home = BlockPos.ZERO;
    public boolean hasHome;

    public TurretEntity(EntityType<? extends PathfinderMob> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
    }

    public static AttributeSupplier.Builder createAttributes(double hp, double speed) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, hp)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public static TurretEntity spawn(ServerLevel level, EntityType<TurretEntity> type, BlockPos pos, Faction f, boolean base) {
        TurretEntity e = type.create(level);
        if (e == null) return null;
        e.setFaction(f);
        e.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        e.home = pos; e.hasHome = true;
        e.restrictTo(pos, 24);
        if (base) e.setPersistenceRequired();
        level.addFreshEntity(e);
        return e;
    }

    @Override protected void defineSynchedData() { super.defineSynchedData(); entityData.define(FACTION, 0); }
    public Faction getFaction() { return Faction.byId(entityData.get(FACTION)); }
    public void setFaction(Faction f) { entityData.set(FACTION, f.ordinal()); }

    @Override
    protected void registerGoals() {
        if (kind == Kind.TANK) {
            goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.7));
            goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6, 0.01f) {
                @Override public boolean canUse() { return TurretEntity.this.getTarget() == null && super.canUse(); }
            });
        }
    }

    /** Tarete bağlı kafa dönüşü gövdeye dolanmasın. */
    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) { @Override public void clientTick() {} };
    }

    @Override public boolean isPushable() { return kind == Kind.TANK; }
    @Override public boolean removeWhenFarAway(double d) { return false; }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (cooldown > 0) cooldown--;
        if (tickCount % 20 == 7) setTarget(findTarget());

        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) return;
        if (kind == Kind.TANK) getNavigation().stop();

        Vec3 aim = t.position().add(0, t.getBbHeight() * 0.5, 0);
        double dx = aim.x - getX(), dz = aim.z - getZ();
        float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float next = Mth.approachDegrees(getYHeadRot(), want, 8f);
        setYHeadRot(next);
        if (kind.lob) {
            setXRot(-65f);
        } else {
            double hd = Math.sqrt(dx * dx + dz * dz);
            setXRot((float) -(Mth.atan2(aim.y - (getY() + getBbHeight() * 0.85), hd) * Mth.RAD_TO_DEG));
        }
        boolean aligned = Math.abs(Mth.degreesDifference(next, want)) < 5f;
        if (cooldown == 0 && aligned && (kind.lob || hasLineOfSight(t))) {
            launchAt(aim, this);
            cooldown = kind.cooldown + random.nextInt(20);
        }
    }

    private LivingEntity findTarget() {
        double r = kind.range;
        AABB box = getBoundingBox().inflate(r, r / 2.0, r);
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == this || !getFaction().isEnemy(e)) return false;
            double d = distanceToSqr(e);
            return d >= (double) kind.minRange * kind.minRange && d <= r * r;
        });
        return list.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
    }

    // ------------------------------------------------------------------ ateş
    public void launchAt(Vec3 aim, net.minecraft.world.entity.Entity owner) {
        Vec3 muzzle = new Vec3(getX(), getY() + getBbHeight() * 0.85, getZ());
        if (!kind.lob) muzzle = muzzle.add(Vec3.directionFromRotation(0, getYHeadRot()).scale(1.4));

        double dx = aim.x - muzzle.x, dz = aim.z - muzzle.z, dy = aim.y - muzzle.y;
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 1.0) return;
        double g = kind.gravity;
        Vec3 vel;
        if (kind.lob) {
            double cos = Math.cos(LOB_ANGLE), sin = Math.sin(LOB_ANGLE);
            double denom = 2 * cos * cos * (d * Math.tan(LOB_ANGLE) - dy);
            if (denom <= 0.01) return;
            double v = Math.sqrt(g * d * d / denom);
            vel = new Vec3(dx / d * v * cos, v * sin, dz / d * v * cos);
        } else {
            double n = d / kind.speed;                       // uçuş süresi (tick)
            double drop = g * n * (n - 1) / 2.0;             // yerçekimi sapması
            vel = new Vec3(dx, dy + drop, dz).normalize().scale(kind.speed);
        }
        double jitter = 0.012 * vel.length();
        vel = vel.add(random.nextGaussian() * jitter, random.nextGaussian() * jitter, random.nextGaussian() * jitter);

        ArtilleryShell shell = new ArtilleryShell(ModEntities.SHELL.get(), level());
        shell.setOwner(owner);
        shell.setPos(muzzle);
        shell.setPower(kind.power);
        shell.setGravity((float) g);
        shell.setDeltaMovement(vel);
        level().addFreshEntity(shell);

        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0f, 1.6f);
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 8, 0.2, 0.2, 0.2, 0.02);
    }

    /** SHIFT + sağ tık: manuel ateş. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()) return super.mobInteract(player, hand);
        if (!level().isClientSide) {
            if (cooldown > 0) {
                player.displayClientMessage(Component.translatable("msg.mbw.cooldown"), true);
            } else {
                Vec3 eye = player.getEyePosition();
                Vec3 end = eye.add(player.getLookAngle().scale(140));
                BlockHitResult hit = level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                Vec3 aim = hit.getLocation();
                float yaw = (float) (Mth.atan2(aim.z - getZ(), aim.x - getX()) * Mth.RAD_TO_DEG) - 90f;
                setYHeadRot(yaw);
                launchAt(aim, player);
                cooldown = kind.cooldown;
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Faction", getFaction().ordinal());
        t.putBoolean("HasHome", hasHome);
        t.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(FACTION, t.getInt("Faction"));
        hasHome = t.getBoolean("HasHome");
        home = BlockPos.of(t.getLong("Home"));
        if (hasHome) restrictTo(home, 24);
    }
}
