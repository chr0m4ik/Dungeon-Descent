package com.example.dungeon.model.enemies;

import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.StatusEffect;

public class Troll extends Enemy {
    public Troll(int x, int y) {
        super("Тролль", "T", x, y, 30, 10, 4, 12, 25);
    }
    @Override public String getTexturePath() { return "mobs/troll.png"; }
    @Override public StatusEffect specialAttack() {
        if (Math.random() < 0.20)
            return new StatusEffect(StatusEffect.Type.STUN, 1);
        return null;
    }
}
