package com.mbw.militarybases.world;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public class BaseStructure extends Structure {
    public static final Codec<BaseStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("faction").forGetter(s -> s.faction.id)
    ).apply(i, BaseStructure::new));

    private final Faction faction;

    public BaseStructure(StructureSettings settings, String faction) {
        super(settings);
        this.faction = Faction.byName(faction);
    }

    private int height(GenerationContext ctx, int x, int z) {
        return ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                ctx.heightAccessor(), ctx.randomState());
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int cx = ctx.chunkPos().getMiddleBlockX();
        int cz = ctx.chunkPos().getMiddleBlockZ();
        int sea = ctx.chunkGenerator().getSeaLevel();

        int y = height(ctx, cx, cz);
        int[] corners = {
                height(ctx, cx - 50, cz - 49), height(ctx, cx + 49, cz - 49),
                height(ctx, cx - 50, cz + 48), height(ctx, cx + 49, cz + 48), y };
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int h : corners) { min = Math.min(min, h); max = Math.max(max, h); }
        if (min < sea + 3 || max - min > 14) return Optional.empty(); // okyanus / uçurum olmasın

        Holder<Biome> biome = ctx.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(cx), QuartPos.fromBlock(y), QuartPos.fromBlock(cz), ctx.randomState().sampler());
        Palette palette = Palette.forBiome(biome);

        BlockPos origin = new BlockPos(cx - Layout.CX, y - 1, cz - Layout.CZ);   // y-1 = zemin blok
        BoundingBox bb = new BoundingBox(origin.getX(), origin.getY() - 14, origin.getZ(),
                origin.getX() + Layout.W - 1, origin.getY() + 24, origin.getZ() + Layout.D - 1);

        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz),
                builder -> builder.addPiece(new BasePiece(bb, origin, faction, palette))));
    }

    @Override
    public StructureType<?> type() { return ModStructures.BASE.get(); }
}
