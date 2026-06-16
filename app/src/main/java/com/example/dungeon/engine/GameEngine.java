package com.example.dungeon.engine;

import com.example.dungeon.engine.DungeonGenerator.GeneratedLevel;
import com.example.dungeon.engine.DungeonGenerator.ItemOnMap;
import com.example.dungeon.engine.DungeonGenerator.TrapOnMap;
import com.example.dungeon.map.GameMap;
import com.example.dungeon.map.Room;
import com.example.dungeon.map.Tile;
import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.Item;
import com.example.dungeon.model.Player;
import com.example.dungeon.model.Potion;
import com.example.dungeon.model.StatusEffect;
import com.example.dungeon.model.Trap;
import com.example.dungeon.model.enemies.DragonBoss;
import com.example.dungeon.achievements.AchievementManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class GameEngine {

    public enum GameState { PLAYING, DEAD, WIN }

    private static final int SIGHT_RADIUS = 5;

    private GameState state = GameState.PLAYING;
    private Player player;
    private GameMap map;
    private List<Enemy> enemies;
    private List<ItemOnMap> items;
    private List<TrapOnMap> traps;

    private final DungeonGenerator generator;
    private Map<Integer, Potion.Effect> potionRegistry;

    private final List<String> log = new ArrayList<>();
    private AchievementManager achievements;
    private int currentFloor = 1;

    public interface GameListener {
        void onStateChanged();
        void onLogMessage(String msg);
    }
    private GameListener listener;

    public GameEngine() {
        generator = new DungeonGenerator(System.currentTimeMillis());
        potionRegistry = generator.getPotionRegistry();
        startFloor(1);
    }

    public void setListener(GameListener l) { this.listener = l; }
    public void setAchievementManager(AchievementManager am) { this.achievements = am; }

    // ── Этаж ──────────────────────────────────────────────────────────────

    private void startFloor(int floor) {
        currentFloor = floor;
        GeneratedLevel level = generator.generate(floor);
        map     = level.map;
        enemies = level.enemies;
        items   = level.items;
        traps   = level.traps;

        if (player == null) {
            player = new Player(level.startX, level.startY);
        } else {
            player.setPosition(level.startX, level.startY);
            player.nextFloor();
        }

        updateVisibility();
        addLog(floor == 4 ? "⚠ Жаркое дыхание тьмы..." : "▶ Этаж " + floor + ". Осторожно.");
        notifyUI();
    }

    // ── Действия игрока ────────────────────────────────────────────────────

    public void playerMove(int dx, int dy) {
        if (state != GameState.PLAYING) return;
        if (player.isStunned()) { addLog("💫 Оглушены!"); endPlayerTurn(); return; }
        if (dx == 0 && dy == 0) { endPlayerTurn(); return; }

        int nx = player.getX() + dx;
        int ny = player.getY() + dy;

        Enemy target = getEnemyAt(nx, ny);
        if (target != null) { attackEnemy(target); endPlayerTurn(); return; }

        Tile tile = map.getTile(nx, ny);
        if (!tile.isWalkable()) return;

        player.setPosition(nx, ny);
        checkPickup(nx, ny);
        checkTrap(nx, ny);
        if (state == GameState.DEAD) { notifyUI(); return; }

        if (tile.getType() == Tile.Type.DOOR) applyDoorEffect();

        if (tile.getType() == Tile.Type.STAIRS_DOWN && currentFloor < 4) {
            addLog("🪜 Вы спускаетесь...");
            if (achievements != null) { achievements.onFloorCleared(); achievements.onFloorReached(currentFloor + 1); }
            startFloor(currentFloor + 1);
            return;
        }

        endPlayerTurn();
    }

    public void useItem(Item item) {
        if (state != GameState.PLAYING) return;
        if (item instanceof Potion) {
            applyPotion((Potion) item);
            player.removeItem(item);
        } else if (item.getType() == Item.Type.WEAPON || item.getType() == Item.Type.ARMOR) {
            player.equip(item);
            addLog("🔧 Экипировано: " + item.getName());
        }
        endPlayerTurn();
    }

    // ── Дверь ─────────────────────────────────────────────────────────────

    private void applyDoorEffect() {
        int px = player.getX(), py = player.getY();
        for (Enemy enemy : enemies) {
            if (!enemy.isAggroed() || enemy.isDoorConfused()) continue;
            int dist = Math.abs(px - enemy.getX()) + Math.abs(py - enemy.getY());
            if (dist > 1) {
                enemy.setDoorConfusedTurns(2);
                addLog("🚪 " + enemy.getName() + " потерял вас!");
            }
        }
    }

    // ── Бой ───────────────────────────────────────────────────────────────

    private void attackEnemy(Enemy enemy) {
        int roll = (int)(player.getTotalAttack() * (0.8 + Math.random() * 0.4));
        enemy.takeDamage(roll);
        addLog("⚔ " + enemy.getName() + " -" + roll + " HP"
             + (enemy.isDoorConfused() ? " [замешательство!]" : ""));
        if (!enemy.isAlive()) {
            player.addKill();
            player.addGold(enemy.getGoldDrop());
            addLog("💀 " + enemy.getName() + " повержен! +" + enemy.getGoldDrop() + "💰");
            enemies.remove(enemy);
            if (enemy instanceof DragonBoss) {
                state = GameState.WIN;
                addLog("🏆 ДРАКОН ПОВЕРЖЕН! ПОБЕДА!");
                if (achievements != null) achievements.onBossKilled();
                notifyUI();
            } else {
                if (achievements != null) achievements.onEnemyKilled();
            }
        }
    }

    private void enemyAttackPlayer(Enemy enemy) {
        int roll = (int)(enemy.getAttack() * (0.8 + Math.random() * 0.4));
        StatusEffect special = enemy.specialAttack();
        player.takeDamage(roll);
        addLog("🩸 " + enemy.getName() + " -" + roll + " HP");
        if (achievements != null) achievements.onPlayerTookDamage();
        if (special != null) { player.addStatus(special); addLog("⚡ " + special.getName() + "!"); }
        if (!player.isAlive()) { state = GameState.DEAD; addLog("💀 Вы погибли..."); }
    }

    // ── Зелья ─────────────────────────────────────────────────────────────

    private void applyPotion(Potion potion) {
        if (achievements != null) achievements.onPotionUsed();
        Potion.Effect effect = potionRegistry.get(potion.getPotionId());
        switch (effect) {
            case HEAL:
                player.heal(20); addLog("🧪 " + potion.getName() + " → +20 HP"); break;
            case STRONG_HEAL:
                player.heal(40); addLog("🧪 " + potion.getName() + " → +40 HP!"); break;
            case POISON_SELF:
                player.addStatus(new StatusEffect(StatusEffect.Type.POISON, 4));
                addLog("🧪 " + potion.getName() + " → Яд!"); break;
            case FIRE_SELF:
                player.addStatus(new StatusEffect(StatusEffect.Type.FIRE, 3));
                addLog("🧪 " + potion.getName() + " → Горение!"); break;
            case BOOST_ATTACK:
                player.applyAttackBuff(3, 5); addLog("🧪 Атака усилена!"); break;
            case REVEAL_TRAPS:
                player.revealTrapsOnCurrentMap(map); addLog("🧪 Ловушки раскрыты!"); break;
        }
    }

    // ── Ловушки / подбор ──────────────────────────────────────────────────

    private void checkTrap(int x, int y) {
        for (TrapOnMap tm : traps) {
            if (tm.x==x && tm.y==y && !tm.trap.isTriggered()) {
                tm.trap.trigger();
                map.getTile(x,y).revealTrap();
                int dmg = tm.trap.getDamage();
                player.takeDamageRaw(dmg);
                addLog("⚠ " + tm.trap.getName() + "! -" + dmg + " HP");
                StatusEffect se = tm.trap.createStatusEffect();
                if (se != null) { player.addStatus(se); addLog("⚡ " + se.getName() + "!"); }
                if (!player.isAlive()) { state = GameState.DEAD; addLog("💀 Ловушка убила вас..."); }
            }
        }
    }

    private void checkPickup(int x, int y) {
        for (ItemOnMap im : items) {
            if (im.x==x && im.y==y && !im.picked) {
                if (!player.inventoryFull()) {
                    player.addItem(im.item); im.picked = true;
                    if (map.getTile(x,y).getType() == Tile.Type.CHEST)
                        map.setTileType(x, y, Tile.Type.FLOOR);
                    addLog("📦 " + im.item.getName());
                    if (achievements != null) achievements.onItemPickedUp();
                } else addLog("🎒 Инвентарь полон!");
            }
        }
    }

    // ── Конец хода игрока → AI врагов ─────────────────────────────────────

    private void endPlayerTurn() {
        if (state != GameState.PLAYING) { notifyUI(); return; }

        int stkDmg = player.tickStatusEffects();
        if (stkDmg > 0) {
            addLog("☠ Статус: -" + stkDmg + " HP");
            if (!player.isAlive()) { state=GameState.DEAD; addLog("💀 Статус убил вас..."); notifyUI(); return; }
        }

        for (Enemy enemy : new ArrayList<>(enemies)) {
            if (!enemy.isAlive()) continue;

            int eDmg = enemy.tickStatusEffects();
            if (!enemy.isAlive()) {
                addLog("☠ " + enemy.getName() + " погиб!");
                player.addKill(); player.addGold(enemy.getGoldDrop());
                continue;
            }

            if (enemy.isStunned()) continue;

            // ── Механика двери ─────────────────────────────────────────
            if (enemy.isDoorConfused()) {
                enemy.tickDoorConfusion();
                if (!enemy.isDoorConfused()) {
                    enemy.deaggro();
                    addLog("🚪 " + enemy.getName() + " потерял след.");
                }
                continue;
            }

            // ── Обычный AI ─────────────────────────────────────────────

            if (!enemy.isAggroed()) {
                if (tryAggro(enemy)) {
                    enemy.aggro();
                    addLog("👁 " + enemy.getName() + " заметил вас!");
                } else {
                    wander(enemy);
                    continue;
                }
            }

            int dist = Math.abs(player.getX()-enemy.getX()) + Math.abs(player.getY()-enemy.getY());

            if (dist == 1) {
                // ── АТАКА ──────────────────────────────────────────────
                enemyAttackPlayer(enemy);
                if (state == GameState.DEAD) { notifyUI(); return; }

            } else {
                // ── ДВИЖЕНИЕ + ПОПЫТКА АТАКИ ПОСЛЕ ШАГА ────────────────
                int[] step = bfsStep(enemy.getX(), enemy.getY(), player.getX(), player.getY());
                int nx2 = enemy.getX() + step[0];
                int ny2 = enemy.getY() + step[1];

                boolean moved = false;
                if ((step[0]!=0 || step[1]!=0)
                        && map.getTile(nx2,ny2).isWalkable()
                        && getEnemyAt(nx2,ny2) == null
                        && !(nx2==player.getX() && ny2==player.getY())) {
                    enemy.setX(nx2);
                    enemy.setY(ny2);
                    moved = true;
                }

                if (moved) {
                    int distAfter = Math.abs(player.getX()-enemy.getX())
                                  + Math.abs(player.getY()-enemy.getY());
                    if (distAfter == 1) {
                        enemyAttackPlayer(enemy);
                        if (state == GameState.DEAD) { notifyUI(); return; }
                    }
                }
            }
        }

        enemies.removeIf(e -> !e.isAlive());
        updateVisibility();
        notifyUI();
    }

    // ── AI ────────────────────────────────────────────────────────────────

    private boolean tryAggro(Enemy enemy) {
        int dist = Math.abs(player.getX()-enemy.getX()) + Math.abs(player.getY()-enemy.getY());
        if (dist <= 5) return true;
        if (dist > 14) return false;
        return Math.random() < Math.pow(0.5, dist - 5);
    }

    private void wander(Enemy enemy) {
        List<Room> rooms = map.getRooms();
        int idx = enemy.getRoomIndex();
        if (idx < 0 || idx >= rooms.size()) return;
        Room room = rooms.get(idx);

        boolean atTarget = enemy.getWanderTargetX()==enemy.getX()
                        && enemy.getWanderTargetY()==enemy.getY();
        boolean noTarget = enemy.getWanderTargetX() == -1;

        if (noTarget || atTarget) {
            if (Math.random() < 0.35) return;
            int tx = room.x + 1 + (int)(Math.random() * Math.max(1, room.width  - 2));
            int ty = room.y + 1 + (int)(Math.random() * Math.max(1, room.height - 2));
            enemy.setWanderTarget(tx, ty);
            return;
        }

        int[] step = bfsStep(enemy.getX(), enemy.getY(),
                             enemy.getWanderTargetX(), enemy.getWanderTargetY());
        int nx = enemy.getX()+step[0], ny = enemy.getY()+step[1];
        if ((step[0]!=0||step[1]!=0) && map.getTile(nx,ny).isWalkable()
                && getEnemyAt(nx,ny)==null
                && !(nx==player.getX()&&ny==player.getY())) {
            enemy.setX(nx); enemy.setY(ny);
        } else {
            enemy.clearWanderTarget();
        }
    }

    // ── BFS ───────────────────────────────────────────────────────────────

    private int[] bfsStep(int fx, int fy, int tx, int ty) {
        if (fx==tx && fy==ty) return new int[]{0,0};
        int W=GameMap.WIDTH, H=GameMap.HEIGHT;
        int[] prev = new int[H*W];
        Arrays.fill(prev, -1);
        int start = fy*W+fx;
        prev[start] = start;
        Queue<Integer> q = new LinkedList<>();
        q.add(start);
        int[][] dirs = {{0,-1},{1,0},{0,1},{-1,0}};
        boolean found = false;
        outer:
        while (!q.isEmpty()) {
            int enc = q.poll();
            int cx=enc%W, cy=enc/W;
            for (int[] d : dirs) {
                int nx=cx+d[0], ny=cy+d[1];
                if (nx<0||nx>=W||ny<0||ny>=H) continue;
                int nEnc=ny*W+nx;
                if (prev[nEnc]!=-1) continue;
                if (!map.getTile(nx,ny).isWalkable()) continue;
                if (!(nx==tx&&ny==ty) && getEnemyAt(nx,ny)!=null) continue;
                prev[nEnc]=enc;
                if (nx==tx&&ny==ty) { found=true; break outer; }
                q.add(nEnc);
            }
        }
        if (!found) return new int[]{0,0};
        int cx=tx, cy=ty;
        while (true) {
            int p=prev[cy*W+cx];
            int px2=p%W, py2=p/W;
            if (px2==fx&&py2==fy) return new int[]{cx-fx,cy-fy};
            cx=px2; cy=py2;
        }
    }

    // ── Утилиты ───────────────────────────────────────────────────────────

    private Enemy getEnemyAt(int x, int y) {
        for (Enemy e : enemies) if (e.getX()==x && e.getY()==y && e.isAlive()) return e;
        return null;
    }

    private void updateVisibility() {
        map.updateVisibility(player.getX(), player.getY(), SIGHT_RADIUS);
    }

    private void addLog(String msg) {
        log.add(msg);
        if (log.size() > 8) log.remove(0);
        if (listener != null) listener.onLogMessage(msg);
    }

    private void notifyUI() { if (listener != null) listener.onStateChanged(); }

    // ── Геттеры ───────────────────────────────────────────────────────────
    public GameState getState()       { return state; }
    public Player getPlayer()         { return player; }
    public GameMap getMap()           { return map; }
    public List<Enemy> getEnemies()   { return enemies; }
    public List<ItemOnMap> getItems() { return items; }
    public List<TrapOnMap> getTraps() { return traps; }
    public List<String> getLog()      { return log; }
    public int getCurrentFloor()      { return currentFloor; }
    public Map<Integer, Potion.Effect> getPotionRegistry() { return potionRegistry; }
}
