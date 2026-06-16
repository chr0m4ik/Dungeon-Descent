package com.example.dungeon.engine;

import com.example.dungeon.map.GameMap;
import com.example.dungeon.map.Room;
import com.example.dungeon.map.Tile;
import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.Item;
import com.example.dungeon.model.Potion;
import com.example.dungeon.model.Trap;
import com.example.dungeon.model.enemies.DragonBoss;
import com.example.dungeon.model.enemies.Rat;
import com.example.dungeon.model.enemies.Skeleton;
import com.example.dungeon.model.enemies.Troll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DungeonGenerator {

    private static final int MIN_ROOMS     = 4;
    private static final int MAX_ROOMS     = 9;
    private static final int MIN_ROOM_SIZE = 4;
    private static final int MAX_ROOM_SIZE = 8;

    private final Random rng;
    private final Map<Integer, Potion.Effect> potionRegistry = new HashMap<>();

    public DungeonGenerator(long seed) {
        this.rng = new Random(seed);
        List<Potion.Effect> pool = new ArrayList<>();
        for (Potion.Effect e : Potion.Effect.values()) pool.add(e);
        Collections.shuffle(pool, rng);
        for (int i = 0; i < 6; i++) potionRegistry.put(i, pool.get(i % pool.size()));
    }

    public Map<Integer, Potion.Effect> getPotionRegistry() { return potionRegistry; }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════

    public GeneratedLevel generate(int floor) {
        GameMap map = new GameMap();
        List<Room> rooms = new ArrayList<>();

        int roomCount = MIN_ROOMS + rng.nextInt(MAX_ROOMS - MIN_ROOMS + 1);
        for (int attempt = 0; attempt < 300 && rooms.size() < roomCount; attempt++) {
            int w = MIN_ROOM_SIZE + rng.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1);
            int h = MIN_ROOM_SIZE + rng.nextInt(MAX_ROOM_SIZE - MIN_ROOM_SIZE + 1);
            int x = 1 + rng.nextInt(GameMap.WIDTH  - w - 2);
            int y = 1 + rng.nextInt(GameMap.HEIGHT - h - 2);
            Room c = new Room(x, y, w, h);
            boolean ok = true;
            for (Room r : rooms) if (c.intersects(r)) { ok = false; break; }
            if (ok) { rooms.add(c); carveRoom(map, c); }
        }

        for (int i = 1; i < rooms.size(); i++)
            carveCorridor(map, rooms.get(i - 1), rooms.get(i));

        placeDoors(map, rooms);
        map.setFloor(floor);
        for (Room r : rooms) map.addRoom(r);

        Room lastRoom  = rooms.get(rooms.size() - 1);
        Room startRoom = rooms.get(0);
        if (floor < 4) map.setStairs(lastRoom.centerX(), lastRoom.centerY());

        List<Enemy>   enemies = new ArrayList<>();
        List<ItemOnMap> items = new ArrayList<>();
        List<TrapOnMap> traps = new ArrayList<>();

        for (int i = 1; i < rooms.size(); i++)
            spawnEnemies(enemies, rooms.get(i), i, floor, rooms.size());

        for (int i = 1; i < rooms.size(); i++)
            spawnItems(map, items, rooms.get(i), floor);

        for (int i = 1; i < rooms.size(); i++)
            spawnTraps(map, traps, rooms.get(i), floor);

        spawnGuaranteed(map, items, rooms, floor);

        if (floor == 4)
            enemies.add(new DragonBoss(lastRoom.centerX(), lastRoom.centerY() - 1));

        return new GeneratedLevel(map, enemies, items, traps,
                                  startRoom.centerX(), startRoom.centerY());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════

    private void carveRoom(GameMap map, Room room) {
        for (int y = room.y; y < room.y + room.height; y++)
            for (int x = room.x; x < room.x + room.width; x++)
                map.setTileType(x, y, Tile.Type.FLOOR);
    }

    private void carveCorridor(GameMap map, Room a, Room b) {
        int x1=a.centerX(), y1=a.centerY(), x2=b.centerX(), y2=b.centerY();
        if (rng.nextBoolean()) { carveH(map,x1,x2,y1); carveV(map,y1,y2,x2); }
        else                   { carveV(map,y1,y2,x1); carveH(map,x1,x2,y2); }
    }

    private void carveH(GameMap map, int x1, int x2, int y) {
        for (int x = Math.min(x1,x2); x <= Math.max(x1,x2); x++)
            if (map.getTile(x,y).getType() == Tile.Type.WALL)
                map.setTileType(x, y, Tile.Type.FLOOR);
    }

    private void carveV(GameMap map, int y1, int y2, int x) {
        for (int y = Math.min(y1,y2); y <= Math.max(y1,y2); y++)
            if (map.getTile(x,y).getType() == Tile.Type.WALL)
                map.setTileType(x, y, Tile.Type.FLOOR);
    }

    // ── Двери на стыке коридор↔комната ─────────────────────────────────────

    private void placeDoors(GameMap map, List<Room> rooms) {
        for (int y = 1; y < GameMap.HEIGHT - 1; y++) {
            for (int x = 1; x < GameMap.WIDTH - 1; x++) {
                if (map.getTile(x, y).getType() != Tile.Type.FLOOR) continue;
                if (isInAnyRoom(rooms, x, y)) continue;

                boolean adjRoom = isInAnyRoom(rooms, x, y-1)
                               || isInAnyRoom(rooms, x, y+1)
                               || isInAnyRoom(rooms, x-1, y)
                               || isInAnyRoom(rooms, x+1, y);
                if (!adjRoom) continue;

                boolean horizNeck = map.getTile(x, y-1).getType() == Tile.Type.WALL
                                 && map.getTile(x, y+1).getType() == Tile.Type.WALL;
                boolean vertNeck  = map.getTile(x-1, y).getType() == Tile.Type.WALL
                                 && map.getTile(x+1, y).getType() == Tile.Type.WALL;

                if (horizNeck || vertNeck) {
                    boolean nearDoor = map.getTile(x,y-1).getType()==Tile.Type.DOOR
                                    || map.getTile(x,y+1).getType()==Tile.Type.DOOR
                                    || map.getTile(x-1,y).getType()==Tile.Type.DOOR
                                    || map.getTile(x+1,y).getType()==Tile.Type.DOOR;
                    if (!nearDoor) map.setTileType(x, y, Tile.Type.DOOR);
                }
            }
        }
    }

    private boolean isInAnyRoom(List<Room> rooms, int x, int y) {
        for (Room r : rooms) if (r.contains(x, y)) return true;
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════

    private void spawnEnemies(List<Enemy> enemies, Room room, int roomIdx,
                              int floor, int totalRooms) {
        int count;
        switch (floor) {
            case 1:  count = 1 + (rng.nextInt(3) == 0 ? 1 : 0); break;
            case 2:  count = 1 + rng.nextInt(2);                 break;
            case 3:  count = 1 + rng.nextInt(3);                 break;
            default: count = 1;                                  break;
        }

        for (int i = 0; i < count; i++) {
            int ex = room.x + 1 + rng.nextInt(Math.max(1, room.width  - 2));
            int ey = room.y + 1 + rng.nextInt(Math.max(1, room.height - 2));
            Enemy e = createEnemy(floor, ex, ey);
            e.setRoomIndex(roomIdx);
            enemies.add(e);
        }
    }

    private Enemy createEnemy(int floor, int x, int y) {
        switch (floor) {
            case 1:  return new Rat(x, y);
            case 2:  return rng.nextInt(100) < 60 ? new Rat(x, y) : new Skeleton(x, y);
            case 3:
                int r = rng.nextInt(100);
                if (r < 15) return new Rat(x, y);
                if (r < 55) return new Skeleton(x, y);
                return new Troll(x, y);
            default: return new Skeleton(x, y);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════

    private void spawnItems(GameMap map, List<ItemOnMap> items, Room room, int floor) {
        if (rng.nextInt(5) < 2) {
            int px = room.x + 1 + rng.nextInt(Math.max(1, room.width  - 2));
            int py = room.y + 1 + rng.nextInt(Math.max(1, room.height - 2));
            items.add(new ItemOnMap(new Potion(rng.nextInt(6)), px, py));
        }

        int chestChance = floor == 1 ? 4 : 3;
        if (rng.nextInt(chestChance) == 0) {
            int ix = room.x + 1 + rng.nextInt(Math.max(1, room.width  - 2));
            int iy = room.y + 1 + rng.nextInt(Math.max(1, room.height - 2));
            items.add(new ItemOnMap(randomItem(floor), ix, iy));
            map.setTileType(ix, iy, Tile.Type.CHEST);
        }
    }

    private void spawnGuaranteed(GameMap map, List<ItemOnMap> items,
                                 List<Room> rooms, int floor) {
        int healId = 0, strongHealId = 0;
        for (Map.Entry<Integer, Potion.Effect> e : potionRegistry.entrySet()) {
            if (e.getValue() == Potion.Effect.HEAL) healId = e.getKey();
            if (e.getValue() == Potion.Effect.STRONG_HEAL) strongHealId = e.getKey();
        }

        switch (floor) {
            case 1:
                placeInRoom(items, rooms, 1, new Potion(healId));
                placeInRoom(items, rooms, 2, new Potion(rng.nextInt(6)));
                Item gear1 = rng.nextBoolean() ? Item.dagger() : Item.leatherArmor();
                placeInRoomAsChest(map, items, rooms, 3, gear1);
                break;

            case 2:
                placeInRoom(items, rooms, 1, new Potion(healId));
                Item gear2 = rng.nextBoolean() ? Item.ironSword() : Item.chainMail();
                placeInRoomAsChest(map, items, rooms, 2, gear2);
                break;

            case 3:
                placeInRoom(items, rooms, 1, new Potion(strongHealId));
                placeInRoom(items, rooms, 2, new Potion(healId));
                Item gear3 = rng.nextBoolean() ? Item.steelSword() : Item.chainMail();
                placeInRoomAsChest(map, items, rooms, 3, gear3);
                break;

            case 4:
                placeInRoom(items, rooms, 1, new Potion(healId));
                placeInRoom(items, rooms, 2, new Potion(strongHealId));
                break;
        }
    }

    private void placeInRoom(List<ItemOnMap> items, List<Room> rooms,
                             int roomIdx, Item item) {
        if (roomIdx >= rooms.size()) return;
        Room r = rooms.get(roomIdx);
        int x = r.x + 1 + rng.nextInt(Math.max(1, r.width  - 2));
        int y = r.y + 1 + rng.nextInt(Math.max(1, r.height - 2));
        items.add(new ItemOnMap(item, x, y));
    }

    private void placeInRoomAsChest(GameMap map, List<ItemOnMap> items,
                                     List<Room> rooms, int roomIdx, Item item) {
        if (roomIdx >= rooms.size()) return;
        Room r = rooms.get(roomIdx);
        int x = r.x + 1 + rng.nextInt(Math.max(1, r.width  - 2));
        int y = r.y + 1 + rng.nextInt(Math.max(1, r.height - 2));
        items.add(new ItemOnMap(item, x, y));
        map.setTileType(x, y, Tile.Type.CHEST);
    }

    private Item randomItem(int floor) {
        int roll = rng.nextInt(100);
        switch (floor) {
            case 1:
                return roll < 50 ? Item.dagger() : Item.leatherArmor();
            case 2:
                if (roll < 35) return Item.ironSword();
                if (roll < 65) return Item.leatherArmor();
                return Item.chainMail();
            default:
                if (roll < 40) return Item.steelSword();
                return Item.chainMail();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════

    private void spawnTraps(GameMap map, List<TrapOnMap> traps, Room room, int floor) {
        int chance = floor == 1 ? 10 : floor == 2 ? 3 : 2;
        if (rng.nextInt(chance) != 0 && floor == 1) return;
        if (floor > 1 && rng.nextInt(chance) != 0) return;

        int count = floor == 1 ? 1 : rng.nextInt(floor);
        for (int i = 0; i < count; i++) {
            int tx = room.x + 1 + rng.nextInt(Math.max(1, room.width  - 2));
            int ty = room.y + 1 + rng.nextInt(Math.max(1, room.height - 2));
            Trap.TrapType type = Trap.TrapType.values()[rng.nextInt(Trap.TrapType.values().length)];
            traps.add(new TrapOnMap(new Trap(type), tx, ty));
            map.setTileType(tx, ty, Tile.Type.TRAP);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════════════

    public static class ItemOnMap {
        public final Item item; public final int x, y; public boolean picked = false;
        public ItemOnMap(Item i, int x, int y) { item=i; this.x=x; this.y=y; }
    }
    public static class TrapOnMap {
        public final Trap trap; public final int x, y;
        public TrapOnMap(Trap t, int x, int y) { trap=t; this.x=x; this.y=y; }
    }
    public static class GeneratedLevel {
        public final GameMap map; public final List<Enemy> enemies;
        public final List<ItemOnMap> items; public final List<TrapOnMap> traps;
        public final int startX, startY;
        public GeneratedLevel(GameMap m, List<Enemy> e, List<ItemOnMap> i,
                              List<TrapOnMap> t, int sx, int sy) {
            map=m; enemies=e; items=i; traps=t; startX=sx; startY=sy;
        }
    }
}
