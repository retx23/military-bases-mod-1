package com.mbw.militarybases;

import com.mbw.militarybases.entity.SoldierEntity;
import com.mbw.militarybases.entity.TurretEntity;
import com.mbw.militarybases.event.ForgeEvents;
import com.mbw.militarybases.registry.ModEntities;
import com.mbw.militarybases.registry.ModItems;
import com.mbw.militarybases.registry.ModStructures;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MilitaryBasesMod.MODID)
public class MilitaryBasesMod {
    public static final String MODID = "mbw";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MilitaryBasesMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModStructures.TYPES.register(bus);
        ModStructures.PIECES.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        bus.addListener(this::onAttributes);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }

    private void onAttributes(EntityAttributeCreationEvent e) {
        e.put(ModEntities.SOLDIER.get(), SoldierEntity.createAttributes().build());
        e.put(ModEntities.CANNON.get(), TurretEntity.createAttributes(60, 0.0).build());
        e.put(ModEntities.MORTAR.get(), TurretEntity.createAttributes(50, 0.0).build());
        e.put(ModEntities.TANK.get(), TurretEntity.createAttributes(160, 0.18).build());
    }
}
