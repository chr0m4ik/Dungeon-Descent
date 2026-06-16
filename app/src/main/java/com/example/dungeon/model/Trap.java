package com.example.dungeon.model;

public class Trap {

    public enum TrapType {
        SPIKE,
        FIRE,
        PIT
    }

    private final TrapType trapType;
    private boolean triggered;

    public Trap(TrapType trapType) {
        this.trapType = trapType;
        this.triggered = false;
    }

    public TrapType getTrapType() { return trapType; }
    public boolean isTriggered()  { return triggered; }
    public void trigger()         { this.triggered = true; }

    public String getName() {
        switch (trapType) {
            case SPIKE: return "Ловушка-шипы";
            case FIRE:  return "Огненная ловушка";
            case PIT:   return "Яма";
            default:    return "Ловушка";
        }
    }

    public int getDamage() {
        switch (trapType) {
            case SPIKE: return 10;
            case FIRE:  return 6;
            case PIT:   return 15;
            default:    return 5;
        }
    }

    public StatusEffect createStatusEffect() {
        switch (trapType) {
            case FIRE: return new StatusEffect(StatusEffect.Type.FIRE, 3);
            case PIT:  return new StatusEffect(StatusEffect.Type.STUN, 1);
            default:   return null;
        }
    }
}
