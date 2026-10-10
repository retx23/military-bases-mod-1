package com.mbw.militarybases;

import com.mbw.militarybases.entity.SoldierEntity;
import com.mbw.militarybases.entity.TurretEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public enum Faction {
    RED(0xB02E26, "red"),   // Terörist
    BLUE(0x3C44AA, "blue"); // Güvenli

    public final int color;
    public final String id;

    Faction(int color, String id) { this.color = color; this.id = id; }

    public static Faction byId(int i) { return i == 1 ? BLUE : RED; }
    public static Faction byName(String s) { return "blue".equalsIgnoreCase(s) ? BLUE : RED; }

    public static Faction of(Entity e) {
        if (e instanceof SoldierEntity s) return s.getFaction();
        if (e instanceof TurretEntity t) return t.getFaction();
        return null;
    }

    /** Bu fraksiyon için verilen varlık düşman mı? */
    public boolean isEnemy(LivingEntity e) {
        if (!e.isAlive()) return false;
        Faction f = of(e);
        if (f != null) return f != this;
        if (e instanceof Player p) return this == RED && !p.isCreative() && !p.isSpectator();
        if (e instanceof Enemy) return this == BLUE; // mavi askerler zombi vb. canavarlara da karşı koyar
        return false;
    }
}
