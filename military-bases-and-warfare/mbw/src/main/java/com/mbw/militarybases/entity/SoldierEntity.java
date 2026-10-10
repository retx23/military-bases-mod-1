package com.mbw.militarybases.entity;

import com.mbw.militarybases.Config;
import com.mbw.militarybases.Faction;
import com.mbw.militarybases.Loadout;
import com.mbw.militarybases.compat.TaczCompat;
import com.mbw.militarybases.entity.ai.PatrolGoal;
import com.mbw.militarybases.entity.ai.RaidGoal;
import com.mbw.militarybases.entity.ai.SoldierCombatGoal;
import com.mbw.militarybases.registry.ModEntities;
import com.mbw.militarybases.world.BaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SoldierEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> FACTION = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AIMING = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SHOOTING = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COMMANDER = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // üs / devriye
    public BlockPos home = BlockPos.ZERO;
    public boolean hasHome;
    public int patrolRadius = 20;
    public long baseKey;
    public boolean hasBase;
    private boolean counted;
    // akın
    public BlockPos raidTarget;
    // silah durumu
    private int loadoutIdx;
    private int ammo;
    private int reloadTicks;
    private int shootCooldown;
    private int shootAnim;
    private int lastDraw = -1000;
    private boolean equipped;

    public SoldierEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation g) g.setCanOpenDoors(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ARMOR, 4.0);
    }

    // ------------------------------------------------------------------ spawn helper
    public static SoldierEntity spawn(ServerLevel level, BlockPos pos, Faction f, boolean commander,
                                      Long baseKey, BlockPos home, int radius) {
        SoldierEntity s = ModEntities.SOLDIER.get().create(level);
        if (s == null) return null;
        s.setFaction(f);
        s.setCommander(commander);
        s.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
        if (home != null) { s.home = home; s.hasHome = true; s.patrolRadius = radius; }
        if (baseKey != null) { s.baseKey = baseKey; s.hasBase = true; s.setPersistenceRequired(); }
        s.equip();
        level.addFreshEntity(s);
        return s;
    }

    // ------------------------------------------------------------------ data
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(FACTION, 0);
        entityData.define(AIMING, false);
        entityData.define(SHOOTING, false);
        entityData.define(COMMANDER, false);
    }

    public Faction getFaction() { return Faction.byId(entityData.get(FACTION)); }
    public void setFaction(Faction f) { entityData.set(FACTION, f.ordinal()); }
    public boolean isCommander() { return entityData.get(COMMANDER); }
    public void setCommander(boolean b) {
        entityData.set(COMMANDER, b);
        if (b) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
            setHealth(40.0f);
            setCustomName(Component.translatable("entity.mbw.commander"));
        }
    }
    public void setAiming(boolean a) {
        if (entityData.get(AIMING) != a) {
            entityData.set(AIMING, a);
            if (!level().isClientSide) TaczCompat.aim(this, a);
        }
    }
    public Loadout getLoadout() { return Loadout.get(loadoutIdx); }
    public int getAmmo() { return ammo; }
    public boolean isReloading() { return reloadTicks > 0; }
    public int getShootCooldown() { return shootCooldown; }
    public void setShootCooldown(int t) { shootCooldown = t; }

    // ------------------------------------------------------------------ goals
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SoldierCombatGoal(this));
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
        goalSelector.addGoal(3, new RaidGoal(this));
        goalSelector.addGoal(4, new PatrolGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false,
                e -> getFaction().isEnemy(e)));
    }

    // ------------------------------------------------------------------ equipment
    public void equip() {
        equipped = true;
        loadoutIdx = Loadout.pick(getRandom());
        Loadout l = getLoadout();
        ammo = l.mag();
        ItemStack gun = TaczCompat.createGun(l);
        if (!gun.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, gun);
            setDropChance(EquipmentSlot.MAINHAND, 0.08f);
        }
        int c = getFaction().color;
        setItemSlot(EquipmentSlot.HEAD, dyed(Items.LEATHER_HELMET, c));
        setItemSlot(EquipmentSlot.CHEST, isCommander() ? new ItemStack(Items.IRON_CHESTPLATE) : dyed(Items.LEATHER_CHESTPLATE, c));
        setItemSlot(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, c));
        setItemSlot(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, c));
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET})
            setDropChance(s, 0f);
        TaczCompat.draw(this);
        lastDraw = tickCount;
    }

    private static ItemStack dyed(net.minecraft.world.item.Item item, int color) {
        ItemStack s = new ItemStack(item);
        s.getOrCreateTagElement("display").putInt("color", color);
        return s;
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!equipped) equip();
        if (shootCooldown > 0) shootCooldown--;
        if (shootAnim > 0 && --shootAnim == 0) entityData.set(SHOOTING, false);
        if (reloadTicks > 0 && --reloadTicks == 0) finishReload();
    }

    public void startReload() {
        if (reloadTicks > 0) return;
        reloadTicks = 50;
        setAiming(false);
        TaczCompat.reload(this);
        level().playSound(null, blockPosition(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.HOSTILE, 1.0f, 1.2f);
    }

    private void finishReload() {
        ammo = getLoadout().mag();
        TaczCompat.refill(getMainHandItem(), ammo); // sonsuz cephane
    }

    /** Hedefe nişan alıp tek atış yapar: önce TaCZ, olmazsa yedek hitscan. */
    public void fireAt(LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition().subtract(0, 0.25, 0);
        Vec3 d = to.subtract(from);
        double hd = Math.sqrt(d.x * d.x + d.z * d.z);
        float spread = Config.SOLDIER_SPREAD.get().floatValue() * (1f + (float) (d.length() / 40.0));
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f + (float) random.nextGaussian() * spread;
        float pitch = (float) -(Mth.atan2(d.y, hd) * Mth.RAD_TO_DEG) + (float) random.nextGaussian() * spread;

        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        setXRot(pitch);

        String result = TaczCompat.shoot(this, pitch, yaw);
        boolean tacz = "SUCCESS".equals(result);
        if ("COOL_DOWN".equals(result)) return; // TaCZ kendi atış hızını yönetiyor
        if (!tacz) {
            if (result != null && tickCount - lastDraw > 40) { TaczCompat.draw(this); lastDraw = tickCount; }
            fallbackShot(target, d.length());
        }
        ammo--;
        shootAnim = 6;
        entityData.set(SHOOTING, true);
    }

    private void fallbackShot(LivingEntity target, double dist) {
        if (getSensing().hasLineOfSight(target)) {
            double chance = Mth.clamp(0.8 - dist / 90.0, 0.15, 0.8);
            if (random.nextDouble() < chance) target.hurt(damageSources().mobAttack(this), getLoadout().damage());
        }
        level().playSound(null, blockPosition(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 1.5f, 1.6f);
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(ParticleTypes.CRIT, getX(), getEyeY() - 0.2, getZ(), 2, 0.1, 0.1, 0.1, 0.0);
    }

    // ------------------------------------------------------------------ interaction / death
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isCommander() && getFaction() == Faction.BLUE && hand == InteractionHand.MAIN_HAND) {
            if (!level().isClientSide && player instanceof ServerPlayer sp)
                BaseManager.questInteract(sp, (ServerLevel) level());
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide && hasBase && !counted) {
            counted = true;
            BaseManager.onBaseUnitDied((ServerLevel) level(), baseKey);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) { return !hasBase && !isPersistenceRequired(); }

    // ------------------------------------------------------------------ NBT
    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("Faction", getFaction().ordinal());
        t.putBoolean("Commander", isCommander());
        t.putBoolean("HasHome", hasHome);
        t.putLong("Home", home.asLong());
        t.putInt("PatrolRadius", patrolRadius);
        t.putBoolean("HasBase", hasBase);
        t.putLong("BaseKey", baseKey);
        t.putInt("Loadout", loadoutIdx);
        t.putInt("Ammo", ammo);
        t.putBoolean("Equipped", equipped);
        if (raidTarget != null) t.putLong("RaidTarget", raidTarget.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        entityData.set(FACTION, t.getInt("Faction"));
        entityData.set(COMMANDER, t.getBoolean("Commander"));
        hasHome = t.getBoolean("HasHome");
        home = BlockPos.of(t.getLong("Home"));
        patrolRadius = t.getInt("PatrolRadius");
        hasBase = t.getBoolean("HasBase");
        baseKey = t.getLong("BaseKey");
        loadoutIdx = t.getInt("Loadout");
        ammo = t.getInt("Ammo");
        equipped = t.getBoolean("Equipped");
        raidTarget = t.contains("RaidTarget") ? BlockPos.of(t.getLong("RaidTarget")) : null;
    }

    // ------------------------------------------------------------------ GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "legs", 4, state -> {
            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }));
        controllers.add(new AnimationController<>(this, "arms", 3, state -> {
            if (entityData.get(SHOOTING)) return state.setAndContinue(RawAnimation.begin().thenLoop("shoot"));
            if (entityData.get(AIMING)) return state.setAndContinue(RawAnimation.begin().thenLoop("aim"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("carry"));
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
