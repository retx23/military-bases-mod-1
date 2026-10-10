package com.mbw.militarybases.client;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.entity.TurretEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Top / havan / tank, vanilla bloklardan kurulu geçici model.
 * (İstersen GeckoLib modeliyle değiştir; mantık TurretEntity'de, görselden bağımsız.)
 */
public class TurretRenderer extends EntityRenderer<TurretEntity> {
    public TurretRenderer(EntityRendererProvider.Context ctx) { super(ctx); }

    private void part(PoseStack ps, MultiBufferSource buf, int light, Block b,
                      float x, float y, float z, float sx, float sy, float sz) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.scale(sx, sy, sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(b.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    @Override
    public void render(TurretEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Block team = e.getFaction() == Faction.RED ? Blocks.RED_CONCRETE : Blocks.BLUE_CONCRETE;
        float hull = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        float head = Mth.rotLerp(pt, e.yHeadRotO, e.yHeadRot);
        float pitch = Mth.lerp(pt, e.xRotO, e.getXRot());

        ps.pushPose();
        switch (e.kind) {
            case CANNON -> {
                ps.mulPose(Axis.YP.rotationDegrees(-head));
                part(ps, buf, light, Blocks.POLISHED_BLACKSTONE, -0.6f, 0.0f, -0.6f, 1.2f, 0.35f, 1.2f);
                part(ps, buf, light, team, -0.35f, 0.35f, -0.4f, 0.7f, 0.3f, 0.8f);
                ps.pushPose();
                ps.translate(0, 0.8, 0);
                ps.mulPose(Axis.XP.rotationDegrees(pitch));
                part(ps, buf, light, Blocks.BLACKSTONE, -0.18f, -0.18f, -0.3f, 0.36f, 0.36f, 1.9f);
                ps.popPose();
            }
            case MORTAR -> {
                ps.mulPose(Axis.YP.rotationDegrees(-head));
                part(ps, buf, light, Blocks.POLISHED_BLACKSTONE, -0.45f, 0.0f, -0.45f, 0.9f, 0.2f, 0.9f);
                ps.pushPose();
                ps.translate(0, 0.25, 0);
                ps.mulPose(Axis.XP.rotationDegrees(pitch + 90f));   // dik duran tüp, pitch ile eğilir
                ps.mulPose(Axis.XP.rotationDegrees(-90f));
                part(ps, buf, light, Blocks.BLACKSTONE, -0.15f, -0.15f, -0.2f, 0.3f, 0.3f, 1.4f);
                ps.popPose();
            }
            case TANK -> {
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(-hull));
                part(ps, buf, light, Blocks.BLACK_CONCRETE, -1.4f, 0.0f, -1.6f, 0.6f, 0.7f, 3.2f);
                part(ps, buf, light, Blocks.BLACK_CONCRETE, 0.8f, 0.0f, -1.6f, 0.6f, 0.7f, 3.2f);
                part(ps, buf, light, team, -0.9f, 0.3f, -1.4f, 1.8f, 0.6f, 2.8f);
                ps.popPose();
                ps.mulPose(Axis.YP.rotationDegrees(-head));
                part(ps, buf, light, team, -0.6f, 0.9f, -0.7f, 1.2f, 0.5f, 1.4f);
                ps.pushPose();
                ps.translate(0, 1.15, 0);
                ps.mulPose(Axis.XP.rotationDegrees(pitch));
                part(ps, buf, light, Blocks.BLACKSTONE, -0.12f, -0.12f, 0.2f, 0.24f, 0.24f, 2.2f);
                ps.popPose();
            }
        }
        ps.popPose();
    }

    @Override public ResourceLocation getTextureLocation(TurretEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
}
