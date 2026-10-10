package com.mbw.militarybases.registry;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.MilitaryBasesMod;
import com.mbw.militarybases.entity.SoldierEntity;
import com.mbw.militarybases.entity.TurretEntity;
import com.mbw.militarybases.item.SpawnerItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MilitaryBasesMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MilitaryBasesMod.MODID);

    public static final RegistryObject<Item> RED_SOLDIER = ITEMS.register("red_soldier",
            () -> new SpawnerItem((l, p) -> SoldierEntity.spawn(l, p, Faction.RED, false, null, p, 25)));
    public static final RegistryObject<Item> BLUE_SOLDIER = ITEMS.register("blue_soldier",
            () -> new SpawnerItem((l, p) -> SoldierEntity.spawn(l, p, Faction.BLUE, false, null, p, 25)));
    public static final RegistryObject<Item> RED_COMMANDER = ITEMS.register("red_commander",
            () -> new SpawnerItem((l, p) -> SoldierEntity.spawn(l, p, Faction.RED, true, null, p, 8)));
    public static final RegistryObject<Item> BLUE_COMMANDER = ITEMS.register("blue_commander",
            () -> new SpawnerItem((l, p) -> SoldierEntity.spawn(l, p, Faction.BLUE, true, null, p, 8)));

    public static final RegistryObject<Item> RED_CANNON = turret("red_cannon", ModEntities.CANNON, Faction.RED);
    public static final RegistryObject<Item> BLUE_CANNON = turret("blue_cannon", ModEntities.CANNON, Faction.BLUE);
    public static final RegistryObject<Item> RED_MORTAR = turret("red_mortar", ModEntities.MORTAR, Faction.RED);
    public static final RegistryObject<Item> BLUE_MORTAR = turret("blue_mortar", ModEntities.MORTAR, Faction.BLUE);
    public static final RegistryObject<Item> RED_TANK = turret("red_tank", ModEntities.TANK, Faction.RED);
    public static final RegistryObject<Item> BLUE_TANK = turret("blue_tank", ModEntities.TANK, Faction.BLUE);

    private static RegistryObject<Item> turret(String name, Supplier<EntityType<TurretEntity>> type, Faction f) {
        return ITEMS.register(name, () -> new SpawnerItem((l, p) -> TurretEntity.spawn(l, type.get(), p, f, false)));
    }

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.mbw"))
                    .icon(() -> new ItemStack(RED_SOLDIER.get()))
                    .displayItems((params, out) -> ITEMS.getEntries().forEach(i -> out.accept(i.get())))
                    .build());
}
