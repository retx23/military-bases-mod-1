package com.mbw.militarybases.client;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.MilitaryBasesMod;
import com.mbw.militarybases.entity.SoldierEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class SoldierModel extends GeoModel<SoldierEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(MilitaryBasesMod.MODID, "geo/soldier.geo.json");
    private static final ResourceLocation ANIM = new ResourceLocation(MilitaryBasesMod.MODID, "animations/soldier.animation.json");
    private static final ResourceLocation RED = new ResourceLocation(MilitaryBasesMod.MODID, "textures/entity/soldier_red.png");
    private static final ResourceLocation BLUE = new ResourceLocation(MilitaryBasesMod.MODID, "textures/entity/soldier_blue.png");

    @Override public ResourceLocation getModelResource(SoldierEntity e) { return MODEL; }
    @Override public ResourceLocation getAnimationResource(SoldierEntity e) { return ANIM; }
    @Override public ResourceLocation getTextureResource(SoldierEntity e) { return e.getFaction() == Faction.RED ? RED : BLUE; }

    @Override
    public void setCustomAnimations(SoldierEntity e, long instanceId, AnimationState<SoldierEntity> state) {
        super.setCustomAnimations(e, instanceId, state);
        CoreGeoBone head = getAnimationProcessor().getBone("head");
        if (head != null) {
            EntityModelData d = state.getData(DataTickets.ENTITY_MODEL_DATA);
            head.setRotX(d.headPitch() * Mth.DEG_TO_RAD);
            head.setRotY(d.netHeadYaw() * Mth.DEG_TO_RAD);
        }
    }
}
