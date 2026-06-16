package com.example.dungeon.model;

public class StatusEffect {

    public enum Type {
        POISON,
        FIRE,
        STUN
    }

    private final Type type;
    private int turnsLeft;

    public StatusEffect(Type type, int turns) {
        this.type = type;
        this.turnsLeft = turns;
    }

    public Type getType() { return type; }
    public int getTurnsLeft() { return turnsLeft; }
    public boolean isExpired() { return turnsLeft <= 0; }

    public int tick() {
        if (turnsLeft <= 0) return 0;
        turnsLeft--;
        switch (type) {
            case POISON: return 3;
            case FIRE:   return 5;
            case STUN:   return 0;
            default:     return 0;
        }
    }

    public String getIcon() {
        switch (type) {
            case POISON: return "☠";
            case FIRE:   return "🔥";
            case STUN:   return "💫";
            default:     return "?";
        }
    }

    public String getName() {
        switch (type) {
            case POISON: return "Яд";
            case FIRE:   return "Огонь";
            case STUN:   return "Оглушение";
            default:     return "Неизвестно";
        }
    }
}
