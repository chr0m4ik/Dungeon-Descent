package com.example.dungeon.map;

public class Room {
    public final int x, y, width, height;

    public Room(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int centerX() { return x + width / 2; }
    public int centerY() { return y + height / 2; }

    public boolean intersects(Room other) {
        return x - 1 <= other.x + other.width
            && x + width + 1 >= other.x
            && y - 1 <= other.y + other.height
            && y + height + 1 >= other.y;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
}
