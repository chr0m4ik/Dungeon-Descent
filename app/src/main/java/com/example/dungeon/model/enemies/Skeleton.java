package com.example.dungeon.model.enemies;

import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.StatusEffect;

public class Skeleton extends Enemy {
    public Skeleton(int x, int y) {
        super("Скелет", "s", x, y, 15, 6, 2, 5, 12);
    }
    @Override public String getTexturePath() { return "mobs/skeleton.png"; }
    @Override public StatusEffect specialAttack() { return null; }
}
