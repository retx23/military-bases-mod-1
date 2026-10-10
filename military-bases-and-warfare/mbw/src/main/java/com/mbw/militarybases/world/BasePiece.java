package com.mbw.militarybases.world;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public class BasePiece extends StructurePiece {
    private final BlockPos origin;
    private final Faction faction;
    private final Palette palette;

    public BasePiece(BoundingBox bb, BlockPos origin, Faction faction, Palette palette) {
        super(ModStructures.BASE_PIECE.get(), 0, bb);
        this.origin = origin; this.faction = faction; this.palette = palette;
    }

    public BasePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModStructures.BASE_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("ox"), tag.getInt("oy"), tag.getInt("oz"));
        this.faction = Faction.byId(tag.getInt("faction"));
        this.palette = Palette.values()[tag.getInt("palette")];
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putInt("ox", origin.getX()); tag.putInt("oy", origin.getY()); tag.putInt("oz", origin.getZ());
        tag.putInt("faction", faction.ordinal());
        tag.putInt("palette", palette.ordinal());
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager sm, ChunkGenerator gen, RandomSource rnd,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        new BaseBuilder(level, box, origin, palette, faction, rnd).build();
        // Üssü kayıt kuyruğuna sadece merkez chunk'ı işlenirken ekle (askerler oyuncu yaklaşınca ana thread'de doğar).
        if (box.isInside(origin.offset(Layout.CX, 0, Layout.CZ)))
            BaseData.PENDING.add(new BaseData.Pending(origin, faction));
    }
}
