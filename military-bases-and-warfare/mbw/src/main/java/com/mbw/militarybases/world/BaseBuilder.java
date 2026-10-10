package com.mbw.militarybases.world;

import com.mbw.militarybases.Faction;
import com.mbw.militarybases.MilitaryBasesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.HashSet;
import java.util.Set;

/**
 * Üssün blok blok inşası. Her chunk için ayrı çağrılır; yalnızca o chunk'ın BoundingBox'ı içindeki
 * bloklar yazılır (vanilla yapılarla aynı mantık). Koordinatlar yerel (Layout) koordinatlarıdır.
 */
public class BaseBuilder {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final WorldGenLevel level;
    private final BoundingBox box;
    private final BlockPos origin;
    private final Palette pal;
    private final Faction faction;
    private final RandomSource rnd;

    public BaseBuilder(WorldGenLevel level, BoundingBox box, BlockPos origin, Palette pal, Faction faction, RandomSource rnd) {
        this.level = level; this.box = box; this.origin = origin; this.pal = pal; this.faction = faction; this.rnd = rnd;
    }

    public void build() {
        terrain();
        walls();
        for (int[] t : Layout.TOWERS) tower(t[0], t[1]);
        road();
        commandCenter();
        for (int[] b : Layout.BARRACKS) barracks(b[0], b[1], b[2], b[3]);
        armory();
        for (int[] m : Layout.MORTARS) ring(m[0], m[1], 2, 1, pal.alt);
        for (int[] c : Layout.CANNONS) ring(c[0], c[1], 2, 1, pal.alt);
        for (int[] t : Layout.TANKS) fill(t[0] - 3, 0, t[1] - 3, t[0] + 3, 0, t[1] + 3, Blocks.GRAY_CONCRETE.defaultBlockState());
        flag();
    }

    // ============================================================ yardımcılar
    private void set(int x, int y, int z, BlockState s) {
        BlockPos p = origin.offset(x, y, z);
        if (box.isInside(p)) level.setBlock(p, s, 2);
    }

    private void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int minX = Math.max(x1, box.minX() - ox), maxX = Math.min(x2, box.maxX() - ox);
        int minY = Math.max(y1, box.minY() - oy), maxY = Math.min(y2, box.maxY() - oy);
        int minZ = Math.max(z1, box.minZ() - oz), maxZ = Math.min(z2, box.maxZ() - oz);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    m.set(ox + x, oy + y, oz + z);
                    level.setBlock(m, s, 2);
                }
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }

    private void ring(int cx, int cz, int r, int y, Block b) {
        for (int i = -r; i <= r; i++) {
            set(cx + i, y, cz - r, b); set(cx + i, y, cz + r, b);
            set(cx - r, y, cz + i, b); set(cx + r, y, cz + i, b);
        }
    }

    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    /** Çit hücrelerini komşularına bağlayarak yerleştirir (setBlock bağlantıları otomatik güncellemediği için elle). */
    private void fences(Set<Long> cells, int y) {
        for (long k : cells) {
            int x = (int) (k >> 32), z = (int) k;
            BlockState st = pal.fence.defaultBlockState()
                    .setValue(FenceBlock.NORTH, cells.contains(key(x, z - 1)))
                    .setValue(FenceBlock.SOUTH, cells.contains(key(x, z + 1)))
                    .setValue(FenceBlock.EAST, cells.contains(key(x + 1, z)))
                    .setValue(FenceBlock.WEST, cells.contains(key(x - 1, z)));
            set(x, y, z, st);
        }
    }

    private void door(int x, int y, int z, Direction f) {
        BlockState lo = pal.door.defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
        set(x, y, z, lo);
        set(x, y + 1, z, lo.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void bed(int x, int y, int z, Direction towardHead) {
        Block b = faction == Faction.RED ? Blocks.RED_BED : Blocks.BLUE_BED;
        BlockState foot = b.defaultBlockState().setValue(BedBlock.FACING, towardHead).setValue(BedBlock.PART, BedPart.FOOT);
        set(x, y, z, foot);
        set(x + towardHead.getStepX(), y, z + towardHead.getStepZ(), foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private void chest(int x, int y, int z, Direction facing, String table) {
        BlockPos p = origin.offset(x, y, z);
        if (!box.isInside(p)) return;
        level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainerBlockEntity.setLootTable(level, rnd, p, new ResourceLocation(MilitaryBasesMod.MODID, "chests/" + table));
    }

    /** Dikdörtgen oda: duvar y1..h, iç boşluk y1..h-1, zemin tahta, tavan = duvar katmanı, çatı = slab, siperlik. */
    private void room(int x1, int z1, int x2, int z2, int h, Block wall) {
        fill(x1, 1, z1, x2, h, z2, wall);
        fill(x1 + 1, 1, z1 + 1, x2 - 1, h - 1, z2 - 1, AIR);
        fill(x1 + 1, 0, z1 + 1, x2 - 1, 0, z2 - 1, pal.floor);
        fill(x1, h + 1, z1, x2, h + 1, z2, pal.slab);
        fill(x1, h + 2, z1, x2, h + 2, z1, wall);
        fill(x1, h + 2, z2, x2, h + 2, z2, wall);
        fill(x1, h + 2, z1, x1, h + 2, z2, wall);
        fill(x2, h + 2, z1, x2, h + 2, z2, wall);
    }

    // ============================================================ arazi
    private void terrain() {
        int ox = origin.getX(), oy = origin.getY(), oz = origin.getZ();
        int minX = Math.max(0, box.minX() - ox), maxX = Math.min(Layout.W - 1, box.maxX() - ox);
        int minZ = Math.max(0, box.minZ() - oz), maxZ = Math.min(Layout.D - 1, box.maxZ() - oz);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState top = pal.top.defaultBlockState(), under = pal.under.defaultBlockState();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = 1; y <= 24; y++) {            // üstü temizle (ağaç, tepe)
                    m.set(ox + x, oy + y, oz + z);
                    if (!level.getBlockState(m).isAir()) level.setBlock(m, AIR, 2);
                }
                m.set(ox + x, oy, oz + z);
                level.setBlock(m, top, 2);
                for (int y = -1; y >= -14; y--) {           // altını doldur (çukur / su)
                    m.set(ox + x, oy + y, oz + z);
                    BlockState st = level.getBlockState(m);
                    if (y >= -3 || st.isAir() || st.canBeReplaced() || !st.getFluidState().isEmpty())
                        level.setBlock(m, under, 2);
                }
            }
        }
    }

    // ============================================================ çevre duvarı + kapı
    private void walls() {
        int W = Layout.W, D = Layout.D;
        BlockState wall = pal.wall.defaultBlockState();
        fill(0, 1, 0, W - 1, 4, 0, wall);
        fill(0, 1, D - 1, W - 1, 4, D - 1, wall);
        fill(0, 1, 0, 0, 4, D - 1, wall);
        fill(W - 1, 1, 0, W - 1, 4, D - 1, wall);

        // üstte çit
        Set<Long> cells = new HashSet<>();
        for (int x = 0; x < W; x++) {
            if (x < Layout.GATE_X1 - 1 || x > Layout.GATE_X2 + 1) cells.add(key(x, 0));
            cells.add(key(x, D - 1));
        }
        for (int z = 0; z < D; z++) { cells.add(key(0, z)); cells.add(key(W - 1, z)); }
        fences(cells, 5);

        // kapı
        int g1 = Layout.GATE_X1, g2 = Layout.GATE_X2;
        fill(g1, 1, 0, g2, 4, 0, AIR);
        fill(g1 - 1, 1, 0, g1 - 1, 6, 0, pal.alt);
        fill(g2 + 1, 1, 0, g2 + 1, 6, 0, pal.alt);
        fill(g1, 5, 0, g2, 5, 0, pal.alt);
        set(g1 - 1, 7, 0, Blocks.LANTERN);
        set(g2 + 1, 7, 0, Blocks.LANTERN);
    }

    // ============================================================ gözetleme kuleleri
    private void tower(int x0, int z0) {
        boolean low = z0 < Layout.D / 2;
        BlockState wall = pal.wall.defaultBlockState();
        fill(x0, 1, z0, x0 + 4, 12, z0 + 4, wall);
        fill(x0 + 1, 1, z0 + 1, x0 + 3, 11, z0 + 3, AIR);
        fill(x0 + 1, 0, z0 + 1, x0 + 3, 0, z0 + 3, pal.floor);

        // merdiven (duvara yaslı) ve zemindeki delik
        int lz = low ? z0 + 1 : z0 + 3;
        Direction ladderFacing = low ? Direction.SOUTH : Direction.NORTH;
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, ladderFacing);
        for (int y = 1; y <= 12; y++) set(x0 + 1, y, lz, ladder);

        // kapı (avlu tarafı)
        int dz = low ? z0 + 4 : z0;
        fill(x0 + 2, 1, dz, x0 + 2, 2, dz, AIR);
        // gözetleme yarıkları
        set(x0 + 2, 8, z0, AIR); set(x0, 8, z0 + 2, AIR); set(x0 + 4, 8, z0 + 2, AIR); set(x0 + 2, 8, z0 + 4, AIR);
        set(x0 + 2, 9, dz == z0 ? z0 + 4 : z0, AIR);

        // platform çiti + çatı
        Set<Long> r = new HashSet<>();
        for (int i = 0; i < 5; i++) { r.add(key(x0 + i, z0)); r.add(key(x0 + i, z0 + 4)); r.add(key(x0, z0 + i)); r.add(key(x0 + 4, z0 + i)); }
        fences(r, 13);
        for (int dx : new int[]{0, 4}) for (int dzz : new int[]{0, 4})
            for (int y = 14; y <= 15; y++) set(x0 + dx, y, z0 + dzz, pal.fence);
        fill(x0, 16, z0, x0 + 4, 16, z0 + 4, pal.slab);
        set(x0 + 2, 14, z0 + 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    // ============================================================ yol + lambalar
    private void road() {
        fill(48, 0, 0, 51, 0, 41, pal.road);
        for (int z = 6; z <= 38; z += 8) {
            for (int x : new int[]{47, 52}) {
                set(x, 1, z, pal.fence); set(x, 2, z, pal.fence);
                set(x, 3, z, Blocks.LANTERN);
            }
        }
    }

    // ============================================================ komuta merkezi
    private void commandCenter() {
        int x1 = Layout.COMMAND[0], z1 = Layout.COMMAND[1], x2 = Layout.COMMAND[2], z2 = Layout.COMMAND[3];
        room(x1, z1, x2, z2, 5, pal.wall);
        door(50, 1, z1, Direction.NORTH);
        for (int x : new int[]{44, 47, 53, 56}) for (int y = 2; y <= 3; y++) { set(x, y, z1, Blocks.GLASS); set(x, y, z2, Blocks.GLASS); }
        for (int z : new int[]{46, 52}) for (int y = 2; y <= 3; y++) { set(x1, y, z, Blocks.GLASS); set(x2, y, z, Blocks.GLASS); }
        Block carpet = faction == Faction.RED ? Blocks.RED_CARPET : Blocks.BLUE_CARPET;
        fill(48, 1, z1 + 1, 52, 1, z2 - 1, carpet);
        set(44, 1, 54, Blocks.CARTOGRAPHY_TABLE);
        set(56, 1, 54, Blocks.LECTERN);
        set(44, 1, 46, Blocks.CRAFTING_TABLE);
        set(56, 1, 46, Blocks.BARREL);
        chest(42, 1, 44, Direction.SOUTH, "command");
        chest(58, 1, 44, Direction.SOUTH, "command");
        chest(42, 1, 54, Direction.NORTH, "command");
        set(50, 4, 49, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(45, 4, 49, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(55, 4, 49, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ============================================================ barakalar
    private void barracks(int x1, int z1, int x2, int z2) {
        boolean low = z1 < Layout.D / 2;
        room(x1, z1, x2, z2, 4, pal.wall);
        int mid = (x1 + x2) / 2;
        door(mid, 1, low ? z2 : z1, low ? Direction.SOUTH : Direction.NORTH);
        for (int x = x1 + 3; x <= x2 - 3; x += 3) {
            if (x == mid) continue;
            set(x, 2, z1, Blocks.GLASS); set(x, 2, z2, Blocks.GLASS);
        }
        int head = low ? z1 + 1 : z2 - 1;
        int foot = low ? z1 + 2 : z2 - 2;
        for (int x = x1 + 2; x <= x2 - 2; x += 3) bed(x, 1, foot, low ? Direction.NORTH : Direction.SOUTH);
        chest(x2 - 1, 1, head, low ? Direction.SOUTH : Direction.NORTH, "barracks");
        set(x1 + 1, 3, low ? z2 - 1 : z1 + 1, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ============================================================ cephanelik
    private void armory() {
        int x1 = Layout.ARMORY[0], z1 = Layout.ARMORY[1], x2 = Layout.ARMORY[2], z2 = Layout.ARMORY[3];
        room(x1, z1, x2, z2, 5, pal.alt);
        door(x1, 1, 46, Direction.WEST);
        for (int x = 74; x <= 84; x += 2) chest(x, 1, z1 + 1, Direction.SOUTH, "armory");
        for (int x = 74; x <= 84; x += 2) { set(x, 1, z2 - 1, Blocks.TNT); if (x % 4 == 0) set(x, 2, z2 - 1, Blocks.TNT); }
        for (int z = 44; z <= 48; z += 2) set(x2 - 1, 1, z, Blocks.BARREL);
        set(79, 4, 46, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ============================================================ bayrak
    private void flag() {
        int fx = Layout.FLAG_X, fz = Layout.FLAG_Z, fh = Layout.FLAG_H;
        fill(fx - 1, 1, fz - 1, fx + 1, 1, fz + 1, pal.alt);
        fill(fx, 2, fz, fx, fh, fz, pal.pole);
        fill(fx + 1, fh - 3, fz, fx + 6, fh, fz, flagBlock(faction));
        set(fx, fh + 1, fz, Blocks.LANTERN);
    }

    public static Block flagBlock(Faction f) { return f == Faction.RED ? Blocks.RED_WOOL : Blocks.BLUE_WOOL; }
}
