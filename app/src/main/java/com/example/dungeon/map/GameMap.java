package com.example.dungeon.map;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameMap {
    public static final int WIDTH  = 40;
    public static final int HEIGHT = 30;

    private final Tile[][] tiles;
    private final List<Room> rooms;

    private final int[][] tileVariant;

    private int stairsX, stairsY;

    private int floor = 1;

    public GameMap() {
        tiles        = new Tile[HEIGHT][WIDTH];
        rooms        = new ArrayList<>();
        tileVariant  = new int[HEIGHT][WIDTH];

        Random rng = new Random();
        for (int r = 0; r < HEIGHT; r++)
            for (int c = 0; c < WIDTH; c++) {
                tiles[r][c]      = new Tile(Tile.Type.WALL);
                tileVariant[r][c] = rng.nextInt(3);
            }
    }

    // ── Тайлы ─────────────────────────────────────────────────────────────

    public Tile getTile(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return new Tile(Tile.Type.WALL);
        return tiles[y][x];
    }

    public void setTileType(int x, int y, Tile.Type type) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return;
        tiles[y][x].setType(type);
    }

    public int getTileVariant(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) return 0;
        return tileVariant[y][x];
    }

    // ── Комнаты ───────────────────────────────────────────────────────────

    public void addRoom(Room room) { rooms.add(room); }
    public List<Room> getRooms()   { return rooms; }

    public Room getRoomAt(int x, int y) {
        for (Room room : rooms)
            if (room.contains(x, y)) return room;
        return null;
    }

    // ── Лестница ──────────────────────────────────────────────────────────

    public void setStairs(int x, int y) {
        stairsX = x; stairsY = y;
        setTileType(x, y, Tile.Type.STAIRS_DOWN);
    }

    public int getStairsX() { return stairsX; }
    public int getStairsY() { return stairsY; }

    // ── Этаж ──────────────────────────────────────────────────────────────

    public void setFloor(int f) { this.floor = f; }
    public int  getFloor()      { return floor; }

    public String getWallTexture() {
        return "tiles/wall_" + floor + ".png";
    }

    public String getFloorTexture() {
        return floor == 1 ? "tiles/floor_1.png" : "tiles/floor_2.png";
    }

    // ── Видимость ─────────────────────────────────────────────────────────

    public void updateVisibility(int cx, int cy, int radius) {
        for (int r = 0; r < HEIGHT; r++)
            for (int c = 0; c < WIDTH; c++)
                tiles[r][c].setVisible(false);

        Room playerRoom = getRoomAt(cx, cy);
        if (playerRoom != null) {
            for (int y = playerRoom.y; y < playerRoom.y + playerRoom.height; y++)
                for (int x = playerRoom.x; x < playerRoom.x + playerRoom.width; x++)
                    tiles[y][x].setVisible(true);
        }

        int steps = radius * 8;
        for (int i = 0; i < steps; i++) {
            double angle = 2 * Math.PI * i / steps;
            castRay(cx, cy, angle, radius);
        }

        if (cx >= 0 && cx < WIDTH && cy >= 0 && cy < HEIGHT)
            tiles[cy][cx].setVisible(true);
    }

    private void castRay(int ox, int oy, double angle, int maxDist) {
        double dx = Math.cos(angle);
        double dy = Math.sin(angle);
        double x = ox + 0.5, y = oy + 0.5;

        for (int step = 0; step < maxDist * 2; step++) {
            x += dx * 0.5;
            y += dy * 0.5;

            int tx = (int) x, ty = (int) y;
            if (tx < 0 || tx >= WIDTH || ty < 0 || ty >= HEIGHT) break;

            double dist = Math.sqrt((tx - ox) * (tx - ox) + (ty - oy) * (ty - oy));
            if (dist > maxDist) break;

            tiles[ty][tx].setVisible(true);

            if (tiles[ty][tx].getType() == Tile.Type.WALL) break;
        }
    }
}
