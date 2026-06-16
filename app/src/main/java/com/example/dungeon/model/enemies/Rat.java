package com.example.dungeon.model.enemies;

import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.StatusEffect;

public class Rat extends Enemy {
    public Rat(int x, int y) {
        super("Крыса", "r", x, y, 8, 3, 0, 2, 5);
    }
    @Override public String getTexturePath() { return "mobs/rat.png"; }
    @Override public StatusEffect specialAttack() {
        if (Math.random() < 0.30)
            return new StatusEffect(StatusEffect.Type.POISON, 3);
        return null;
    }
}
