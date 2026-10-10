package com.mbw.militarybases.world;

/**
 * Üs yerleşim planı. Yerel koordinat: (0,0) = üssün kuzey-batı köşesi (min X, min Z).
 * 100 (X) x 99 (Z) blok. Kapı z=0 (kuzey) duvarında, x=46..53.
 */
public final class Layout {
    private Layout() {}

    public static final int W = 100, D = 99;
    public static final int CX = 50, CZ = 49;

    public static final int GATE_X1 = 46, GATE_X2 = 53;

    // {x, z}
    public static final int[][] SOLDIERS = {
            {46, 8}, {54, 8}, {36, 30}, {64, 30}, {50, 36}, {20, 48}, {66, 50},
            {30, 66}, {70, 66}, {50, 70}, {40, 86}, {60, 86}, {15, 32}, {85, 32}
    };
    public static final int[][] CANNONS = {{20, 4}, {80, 4}, {20, 94}, {80, 94}};
    public static final int[][] MORTARS = {{26, 58}, {74, 62}, {50, 64}};
    public static final int[][] TANKS = {{40, 14}, {60, 14}};
    public static final int[] COMMANDER = {50, 49};

    // komuta merkezi, cephanelik
    public static final int[] COMMAND = {40, 42, 60, 56};   // x1,z1,x2,z2
    public static final int[] ARMORY = {72, 40, 86, 52};
    // barakalar: x1,z1,x2,z2
    public static final int[][] BARRACKS = {{8, 12, 22, 21}, {78, 12, 92, 21}, {8, 76, 22, 85}, {78, 76, 92, 85}};
    // kuleler (sol-üst köşe, 5x5)
    public static final int[][] TOWERS = {{1, 1}, {94, 1}, {1, 93}, {94, 93}};

    // bayrak
    public static final int FLAG_X = 55, FLAG_Z = 20, FLAG_H = 16;
}
