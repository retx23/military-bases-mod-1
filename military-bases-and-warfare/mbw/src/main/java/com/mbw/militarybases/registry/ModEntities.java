package com.mbw.militarybases.registry;

import com.mbw.militarybases.MilitaryBasesMod;
import com.mbw.militarybases.entity.ArtilleryShell;
import com.mbw.militarybases.entity.SoldierEntity;
import com.mbw.militarybases.entity.TurretEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MilitaryBasesMod.MODID);

    public static final RegistryObject<EntityType<SoldierEntity>> SOLDIER = ENTITIES.register("soldier",
            () -> EntityType.Builder.<SoldierEntity>of(SoldierEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build("soldier"));

    public static final RegistryObject<EntityType<TurretEntity>> CANNON = ENTITIES.register("cannon",
            () -> EntityType.Builder.<TurretEntity>of((t, l) -> new TurretEntity(t, l, TurretEntity.Kind.CANNON), MobCategory.MISC)
                    .sized(1.4f, 1.2f).clientTrackingRange(12).build("cannon"));

    public static final RegistryObject<EntityType<TurretEntity>> MORTAR = ENTITIES.register("mortar",
            () -> EntityType.Builder.<TurretEntity>of((t, l) -> new TurretEntity(t, l, TurretEntity.Kind.MORTAR), MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(12).build("mortar"));

    public static final RegistryObject<EntityType<TurretEntity>> TANK = ENTITIES.register("tank",
            () -> EntityType.Builder.<TurretEntity>of((t, l) -> new TurretEntity(t, l, TurretEntity.Kind.TANK), MobCategory.MISC)
                    .sized(3.0f, 2.0f).clientTrackingRange(14).build("tank"));

    public static final RegistryObject<EntityType<ArtilleryShell>> SHELL = ENTITIES.register("shell",
            () -> EntityType.Builder.<ArtilleryShell>of(ArtilleryShell::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(16).updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true).build("shell"));
}
