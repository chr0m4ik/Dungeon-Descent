package com.example.dungeon.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Enemy {

    protected String name;
    protected String symbol;
    protected int x, y;
    protected int maxHp, hp;
    protected int attack, defense;
    protected int goldDrop, expDrop;
    protected boolean aggroed = false;

    private int roomIndex = -1;
    private int wanderTargetX = -1, wanderTargetY = -1;

    private int doorConfusedTurns = 0;

    protected final List<StatusEffect> statusEffects = new ArrayList<>();

    public Enemy(String name, String symbol, int x, int y,
                 int maxHp, int attack, int defense, int goldDrop, int expDrop) {
        this.name=name; this.symbol=symbol; this.x=x; this.y=y;
        this.maxHp=maxHp; this.hp=maxHp; this.attack=attack; this.defense=defense;
        this.goldDrop=goldDrop; this.expDrop=expDrop;
    }

    public abstract String getTexturePath();

    // ── Статус-эффекты ─────────────────────────────────────────────────────
    public void addStatus(StatusEffect e) { statusEffects.add(e); }
    public List<StatusEffect> getStatusEffects() { return statusEffects; }

    public boolean isStunned() {
        for (StatusEffect se : statusEffects)
            if (se.getType() == StatusEffect.Type.STUN && !se.isExpired()) return true;
        return false;
    }

    public int tickStatusEffects() {
        int total = 0;
        Iterator<StatusEffect> it = statusEffects.iterator();
        while (it.hasNext()) {
            StatusEffect se = it.next();
            int d = se.tick();
            hp = Math.max(0, hp - d);
            total += d;
            if (se.isExpired()) it.remove();
        }
        return total;
    }

    // ── Бой ────────────────────────────────────────────────────────────────
    public void takeDamage(int amount) {
        int reduced = Math.max(0, amount - defense);
        hp = Math.max(0, hp - reduced);
    }
    public StatusEffect specialAttack() { return null; }

    // ── Дверь (confusion) ──────────────────────────────────────────────────
    public int getDoorConfusedTurns()       { return doorConfusedTurns; }
    public void setDoorConfusedTurns(int t) { doorConfusedTurns = t; }
    public void tickDoorConfusion()         { if (doorConfusedTurns > 0) doorConfusedTurns--; }
    public boolean isDoorConfused()         { return doorConfusedTurns > 0; }

    public void deaggro() {
        aggroed = false;
        doorConfusedTurns = 0;
        clearWanderTarget();
    }

    // ── AI ────────────────────────────────────────────────────────────────
    public int getRoomIndex()  { return roomIndex; }
    public void setRoomIndex(int i) { roomIndex = i; }
    public int getWanderTargetX() { return wanderTargetX; }
    public int getWanderTargetY() { return wanderTargetY; }
    public void setWanderTarget(int x, int y) { wanderTargetX=x; wanderTargetY=y; }
    public void clearWanderTarget()            { wanderTargetX=-1; wanderTargetY=-1; }

    // ── Геттеры ───────────────────────────────────────────────────────────
    public String getName()    { return name; }
    public String getSymbol()  { return symbol; }
    public int getX()          { return x; }
    public int getY()          { return y; }
    public void setX(int x)    { this.x=x; }
    public void setY(int y)    { this.y=y; }
    public int getHp()         { return hp; }
    public int getMaxHp()      { return maxHp; }
    public boolean isAlive()   { return hp > 0; }
    public int getAttack()     { return attack; }
    public int getDefense()    { return defense; }
    public int getGoldDrop()   { return goldDrop; }
    public int getExpDrop()    { return expDrop; }
    public boolean isAggroed() { return aggroed; }
    public void aggro()        { aggroed = true; doorConfusedTurns = 0; }
}
