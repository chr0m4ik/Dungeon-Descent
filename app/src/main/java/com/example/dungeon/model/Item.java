package com.example.dungeon.model;

public class Item {

    public enum Type { WEAPON, ARMOR, POTION, SCROLL }

    protected String name;
    protected String symbol;
    protected Type type;
    protected int attackBonus;
    protected int defenseBonus;
    protected String description;
    protected String texturePath;

    public Item(String name, String symbol, Type type,
                int attackBonus, int defenseBonus,
                String description, String texturePath) {
        this.name        = name;
        this.symbol      = symbol;
        this.type        = type;
        this.attackBonus = attackBonus;
        this.defenseBonus= defenseBonus;
        this.description = description;
        this.texturePath = texturePath;
    }

    public String getName()        { return name; }
    public String getSymbol()      { return symbol; }
    public Type getType()          { return type; }
    public int getAttackBonus()    { return attackBonus; }
    public int getDefenseBonus()   { return defenseBonus; }
    public String getDescription() { return description; }
    public String getTexturePath() { return texturePath; }

    public String getStatsLine() {
        if (attackBonus > 0)  return "⚔ Атака: +" + attackBonus;
        if (defenseBonus > 0) return "🛡 Защита: +" + defenseBonus;
        return "";
    }

    public String getActionLabel() {
        switch (type) {
            case WEAPON:
            case ARMOR:  return "Экипировать";
            case POTION: return "Выпить";
            default:     return "Использовать";
        }
    }

    // ── Готовые предметы ──────────────────────────────────────────────────

    public static Item dagger() {
        return new Item("Кинжал", "🗡", Type.WEAPON, 2, 0,
            "Быстрый, но слабый. Хорошо подходит новичкам.",
            "items/dagger.png");
    }

    public static Item ironSword() {
        return new Item("Железный меч", "⚔", Type.WEAPON, 4, 0,
            "Надёжный меч из кованого железа.",
            "items/sword_iron.png");
    }

    public static Item steelSword() {
        return new Item("Стальной меч", "⚔", Type.WEAPON, 8, 0,
            "Острый клинок из лучшей стали. Серьёзное оружие.",
            "items/sword_steel.png");
    }

    public static Item leatherArmor() {
        return new Item("Кожаный доспех", "🛡", Type.ARMOR, 0, 2,
            "Лёгкая защита. Не сковывает движения.",
            "items/armor_leather.png");
    }

    public static Item chainMail() {
        return new Item("Кольчуга", "🛡", Type.ARMOR, 0, 5,
            "Прочная кольчуга из металлических колец.",
            "items/armor_chain.png");
    }
}
