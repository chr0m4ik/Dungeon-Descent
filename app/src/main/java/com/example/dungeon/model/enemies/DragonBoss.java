package com.example.dungeon.model.enemies;

import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.StatusEffect;

public class DragonBoss extends Enemy {
    private int turnCount = 0;

    public DragonBoss(int x, int y) {
        super("Дракон", "D", x, y, 120, 18, 6, 100, 200);
    }

    @Override public String getTexturePath() { return "mobs/dragon.png"; }

    @Override
    public StatusEffect specialAttack() {
        turnCount++;
        if (turnCount % 2 == 0)
            return new StatusEffect(StatusEffect.Type.FIRE, 3);
        return null;
    }

    public boolean isEnraged() { return hp < maxHp / 2; }

    @Override
    public int getAttack() { return isEnraged() ? attack + 5 : attack; }
}
