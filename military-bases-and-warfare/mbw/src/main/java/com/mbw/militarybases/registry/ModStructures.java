package com.mbw.militarybases.registry;

import com.mbw.militarybases.MilitaryBasesMod;
import com.mbw.militarybases.world.BasePiece;
import com.mbw.militarybases.world.BaseStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MilitaryBasesMod.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MilitaryBasesMod.MODID);

    public static final RegistryObject<StructureType<BaseStructure>> BASE =
            TYPES.register("military_base", () -> (StructureType<BaseStructure>) () -> BaseStructure.CODEC);

    public static final RegistryObject<StructurePieceType> BASE_PIECE =
            PIECES.register("base_piece", () -> (StructurePieceType) BasePiece::new);
}
