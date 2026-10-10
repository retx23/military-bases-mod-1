package com.mbw.militarybases.client;

import com.mbw.militarybases.MilitaryBasesMod;
import com.mbw.militarybases.registry.ModEntities;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MilitaryBasesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.SOLDIER.get(), SoldierRenderer::new);
        e.registerEntityRenderer(ModEntities.CANNON.get(), TurretRenderer::new);
        e.registerEntityRenderer(ModEntities.MORTAR.get(), TurretRenderer::new);
        e.registerEntityRenderer(ModEntities.TANK.get(), TurretRenderer::new);
        e.registerEntityRenderer(ModEntities.SHELL.get(), NoopRenderer::new);
    }
}
