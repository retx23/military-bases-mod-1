package com.mbw.militarybases.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Yaratıcı moddan asker / top / tank doğurmak için basit "spawner" item'ı. */
public class SpawnerItem extends Item {
    @FunctionalInterface
    public interface Spawner { void spawn(ServerLevel level, BlockPos pos); }

    private final Spawner spawner;

    public SpawnerItem(Spawner spawner) {
        super(new Item.Properties().stacksTo(64));
        this.spawner = spawner;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        spawner.spawn((ServerLevel) level, pos);
        if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
