package com.example.dungeon.map;

public class Tile {
    public enum Type {
        WALL,
        FLOOR,
        DOOR,
        STAIRS_DOWN,
        TRAP,
        CHEST
    }

    private Type type;
    private boolean visible;
    private boolean explored;
    private boolean trapRevealed;

    public Tile(Type type) {
        this.type = type;
        this.visible = false;
        this.explored = false;
        this.trapRevealed = false;
    }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) this.explored = true;
    }

    public boolean isExplored() { return explored; }

    public boolean isTrapRevealed() { return trapRevealed; }
    public void revealTrap() { this.trapRevealed = true; }

    public boolean isWalkable() {
        return type == Type.FLOOR
            || type == Type.DOOR
            || type == Type.STAIRS_DOWN
            || type == Type.TRAP
            || type == Type.CHEST;
    }

    public String getSymbol() {
        switch (type) {
            case WALL:        return "█";
            case FLOOR:       return "·";
            case DOOR:        return "+";
            case STAIRS_DOWN: return ">";
            case TRAP:        return trapRevealed ? "^" : "·";
            case CHEST:       return "$";
            default:          return " ";
        }
    }
}
