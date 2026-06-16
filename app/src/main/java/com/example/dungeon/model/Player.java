package com.example.dungeon.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Player {

    // ── Позиция ────────────────────────────────────────────────────────────
    private int x, y;

    // ── Базовые характеристики ─────────────────────────────────────────────
    private int maxHp;
    private int hp;
    private int baseAttack;
    private int baseDefense;

    private int attackBuffAmount;
    private int attackBuffTurns;

    // ── Экипировка ─────────────────────────────────────────────────────────
    private Item equippedWeapon;
    private Item equippedArmor;

    // ── Инвентарь ──────────────────────────────────────────────────────────
    private final List<Item> inventory;
    private static final int MAX_INVENTORY = 10;

    // ── Статус-эффекты ─────────────────────────────────────────────────────
    private final List<StatusEffect> statusEffects;

    // ── Статистика ─────────────────────────────────────────────────────────
    private int floor;
    private int killCount;
    private int goldCollected;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.maxHp = 30;
        this.hp = 30;
        this.baseAttack = 5;
        this.baseDefense = 1;
        this.inventory = new ArrayList<>();
        this.statusEffects = new ArrayList<>();
        this.floor = 1;
    }

    // ── Движение ───────────────────────────────────────────────────────────
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public int getX() { return x; }
    public int getY() { return y; }

    // ── HP ─────────────────────────────────────────────────────────────────
    public int getHp()    { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isAlive() { return hp > 0; }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void takeDamage(int amount) {
        int reduced = Math.max(0, amount - getTotalDefense());
        hp = Math.max(0, hp - reduced);
    }

    public void takeDamageRaw(int amount) {
        hp = Math.max(0, hp - amount);
    }

    // ── Урон / защита ──────────────────────────────────────────────────────
    public int getTotalAttack() {
        int weapon = equippedWeapon != null ? equippedWeapon.getAttackBonus() : 0;
        return baseAttack + weapon + attackBuffAmount;
    }

    public int getTotalDefense() {
        int armor = equippedArmor != null ? equippedArmor.getDefenseBonus() : 0;
        return baseDefense + armor;
    }

    // ── Статус-эффекты ─────────────────────────────────────────────────────
    public void addStatus(StatusEffect effect) {
        for (StatusEffect se : statusEffects) {
            if (se.getType() == effect.getType()) {
                break;
            }
        }
        statusEffects.add(effect);
    }

    public List<StatusEffect> getStatusEffects() { return statusEffects; }

    public boolean isStunned() {
        for (StatusEffect se : statusEffects)
            if (se.getType() == StatusEffect.Type.STUN && !se.isExpired()) return true;
        return false;
    }

    public int tickStatusEffects() {
        int totalDmg = 0;
        Iterator<StatusEffect> it = statusEffects.iterator();
        while (it.hasNext()) {
            StatusEffect se = it.next();
            int dmg = se.tick();
            hp = Math.max(0, hp - dmg);
            totalDmg += dmg;
            if (se.isExpired()) it.remove();
        }
        if (attackBuffTurns > 0) {
            attackBuffTurns--;
            if (attackBuffTurns == 0) attackBuffAmount = 0;
        }
        return totalDmg;
    }

    // ── Инвентарь ──────────────────────────────────────────────────────────
    public List<Item> getInventory() { return inventory; }

    public boolean addItem(Item item) {
        if (inventory.size() >= MAX_INVENTORY) return false;
        inventory.add(item);
        return true;
    }

    public void removeItem(Item item) { inventory.remove(item); }

    public boolean inventoryFull() { return inventory.size() >= MAX_INVENTORY; }

    // ── Экипировка ─────────────────────────────────────────────────────────
    public void equip(Item item) {
        if (item.getType() == Item.Type.WEAPON) {
            if (equippedWeapon != null) inventory.add(equippedWeapon);
            equippedWeapon = item;
            inventory.remove(item);
        } else if (item.getType() == Item.Type.ARMOR) {
            if (equippedArmor != null) inventory.add(equippedArmor);
            equippedArmor = item;
            inventory.remove(item);
        }
    }

    public Item getEquippedWeapon()  { return equippedWeapon; }
    public Item getEquippedArmor()   { return equippedArmor; }

    // ── Бафф атаки ─────────────────────────────────────────────────────────
    public void applyAttackBuff(int amount, int turns) {
        attackBuffAmount = amount;
        attackBuffTurns  = turns;
    }

    // ── Статистика ─────────────────────────────────────────────────────────
    public int getFloor()    { return floor; }
    public void nextFloor()  { floor++; }
    public int getKillCount()  { return killCount; }
    public void addKill()      { killCount++; }
    public int getGold()       { return goldCollected; }
    public void addGold(int g) { goldCollected += g; }

    // ── Показать ловушки (эффект зелья) ────────────────────────────────────
    public void revealTrapsOnCurrentMap(com.example.dungeon.map.GameMap map) {
        for (int y = 0; y < com.example.dungeon.map.GameMap.HEIGHT; y++)
            for (int x = 0; x < com.example.dungeon.map.GameMap.WIDTH; x++)
                if (map.getTile(x, y).getType() == com.example.dungeon.map.Tile.Type.TRAP)
                    map.getTile(x, y).revealTrap();
    }
}
