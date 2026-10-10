package com.mbw.militarybases.client;

import com.mbw.militarybases.entity.SoldierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class SoldierRenderer extends GeoEntityRenderer<SoldierEntity> {
    public SoldierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SoldierModel());
        this.shadowRadius = 0.4f;
        addRenderLayer(new BlockAndItemGeoLayer<>(this) {
            @Nullable @Override
            protected ItemStack getStackForBone(GeoBone bone, SoldierEntity a) {
                return bone.getName().equals("weapon") ? a.getMainHandItem() : null;
            }
            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, SoldierEntity a) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }
            @Override
            protected void renderStackForBone(PoseStack ps, GeoBone bone, ItemStack stack, SoldierEntity a,
                                              MultiBufferSource buf, float pt, int light, int overlay) {
                // Silahın elde duruş açısı: gerekirse burada ayarla (Blockbench'teki "weapon" kemiğiyle birlikte).
                ps.mulPose(Axis.XP.rotationDegrees(-90f));
                ps.translate(0.0, 0.1, 0.0);
                super.renderStackForBone(ps, bone, stack, a, buf, pt, light, overlay);
            }
        });
    }
}
