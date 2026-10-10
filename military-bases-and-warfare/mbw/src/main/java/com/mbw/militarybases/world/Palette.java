package com.mbw.militarybases.world;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Biyoma göre üs malzemeleri. */
public enum Palette {
    DEFAULT(Blocks.STONE_BRICKS, Blocks.COBBLESTONE, Blocks.SPRUCE_PLANKS, Blocks.GRASS_BLOCK, Blocks.DIRT,
            Blocks.STONE_BRICK_SLAB, Blocks.OAK_FENCE, Blocks.OAK_DOOR, Blocks.DIRT_PATH, Blocks.STRIPPED_OAK_LOG),
    FOREST(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.OAK_PLANKS, Blocks.GRASS_BLOCK, Blocks.DIRT,
            Blocks.SPRUCE_SLAB, Blocks.DARK_OAK_FENCE, Blocks.DARK_OAK_DOOR, Blocks.DIRT_PATH, Blocks.STRIPPED_DARK_OAK_LOG),
    DESERT(Blocks.CUT_SANDSTONE, Blocks.SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SAND, Blocks.SANDSTONE,
            Blocks.SANDSTONE_SLAB, Blocks.ACACIA_FENCE, Blocks.ACACIA_DOOR, Blocks.SMOOTH_SANDSTONE, Blocks.STRIPPED_ACACIA_LOG),
    SNOW(Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.SNOW_BLOCK, Blocks.DIRT,
            Blocks.SPRUCE_SLAB, Blocks.SPRUCE_FENCE, Blocks.SPRUCE_DOOR, Blocks.GRAVEL, Blocks.STRIPPED_SPRUCE_LOG),
    BADLANDS(Blocks.TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.ACACIA_PLANKS, Blocks.RED_SAND, Blocks.RED_SANDSTONE,
            Blocks.RED_SANDSTONE_SLAB, Blocks.ACACIA_FENCE, Blocks.ACACIA_DOOR, Blocks.RED_SANDSTONE, Blocks.STRIPPED_ACACIA_LOG);

    public final Block wall, alt, floor, top, under, slab, fence, door, road, pole;

    Palette(Block wall, Block alt, Block floor, Block top, Block under, Block slab, Block fence, Block door, Block road, Block pole) {
        this.wall = wall; this.alt = alt; this.floor = floor; this.top = top; this.under = under;
        this.slab = slab; this.fence = fence; this.door = door; this.road = road; this.pole = pole;
    }

    public static Palette forBiome(Holder<Biome> b) {
        if (b.is(BiomeTags.IS_BADLANDS)) return BADLANDS;
        if (b.is(Biomes.DESERT) || b.is(BiomeTags.IS_BEACH)) return DESERT;
        if (b.value().getBaseTemperature() < 0.2f) return SNOW;
        if (b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA) || b.is(BiomeTags.IS_JUNGLE)) return FOREST;
        return DEFAULT;
    }
}
