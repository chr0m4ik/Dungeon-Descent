package com.example.dungeon.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

import com.example.dungeon.engine.DungeonGenerator.ItemOnMap;
import com.example.dungeon.engine.GameEngine;
import com.example.dungeon.map.GameMap;
import com.example.dungeon.map.Tile;
import com.example.dungeon.model.Enemy;
import com.example.dungeon.model.Player;
import com.example.dungeon.model.enemies.DragonBoss;

import java.util.HashSet;
import java.util.Set;

public class GameView extends View {

    public static final int CELL = 32;

    private GameEngine engine;
    private TextureManager tex;

    private final Paint fillPaint = new Paint();
    private final Paint bmpFull   = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint bmpDim    = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint bmpOver   = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint txtPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dst = new RectF();

    private int camX, camY;

    public GameView(Context context) {
        super(context);
        tex = TextureManager.get(context);

        ColorMatrix dim = new ColorMatrix();
        dim.setScale(0.35f, 0.35f, 0.35f, 1f);
        bmpDim.setColorFilter(new ColorMatrixColorFilter(dim));

        txtPaint.setTypeface(Typeface.MONOSPACE);
        txtPaint.setTextSize(CELL * 0.72f);
        txtPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setEngine(GameEngine e) { this.engine = e; }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        if (engine == null) return;

        GameMap map    = engine.getMap();
        Player  player = engine.getPlayer();

        int visW = getWidth()  / CELL + 2;
        int visH = getHeight() / CELL + 2;

        camX = clamp(player.getX() - getWidth()/CELL/2,  0, Math.max(0, GameMap.WIDTH  - visW + 1));
        camY = clamp(player.getY() - getHeight()/CELL/2, 0, Math.max(0, GameMap.HEIGHT - visH + 1));

        Set<Integer> openDoors = buildOpenDoors(map, player);

        // ── 1. Тайлы пола и стен ─────────────────────────────────────────
        for (int sy = 0; sy < visH; sy++) {
            for (int sx = 0; sx < visW; sx++) {
                int mx = camX + sx, my = camY + sy;
                if (mx < 0 || mx >= GameMap.WIDTH || my < 0 || my >= GameMap.HEIGHT) continue;
                Tile tile = map.getTile(mx, my);
                if (!tile.isExplored()) continue;

                boolean visible   = tile.isVisible();
                int     variant   = map.getTileVariant(mx, my);
                boolean isDoorOpen = openDoors.contains(my * GameMap.WIDTH + mx);

                drawBaseTile(canvas, map, tile, sx, sy, variant, visible, isDoorOpen);
            }
        }

        // ── 2. Overlay: сундуки, лестница, ловушки (поверх пола) ─────────
        for (int sy = 0; sy < visH; sy++) {
            for (int sx = 0; sx < visW; sx++) {
                int mx = camX + sx, my = camY + sy;
                if (mx < 0 || mx >= GameMap.WIDTH || my < 0 || my >= GameMap.HEIGHT) continue;
                Tile tile = map.getTile(mx, my);
                if (!tile.isVisible()) continue;

                String overlayPath = getOverlayPath(tile);
                if (overlayPath == null) continue;
                Bitmap bmp = tex.loadTransparent(overlayPath);
                if (bmp != null) drawBitmapPaint(canvas, bmp, sx, sy, 1, 1, bmpOver);
                else {
                    txtPaint.setColor(tileTextColor(tile));
                    canvas.drawText(tile.getSymbol(), sx*CELL+CELL/2f, sy*CELL+CELL*0.76f, txtPaint);
                }
            }
        }

        // ── 3. Предметы (только видимые) ─────────────────────────────────
        for (ItemOnMap im : engine.getItems()) {
            if (im.picked) continue;
            if (!map.getTile(im.x, im.y).isVisible()) continue;
            int sx = im.x - camX, sy = im.y - camY;
            if (!onScreen(sx, sy, visW, visH)) continue;

            String tp = im.item.getTexturePath();
            Bitmap bmp = tp != null ? tex.loadTransparent(tp) : null;
            if (bmp != null) drawBitmapPaint(canvas, bmp, sx, sy, 1, 1, bmpOver);
            else {
                boolean isPotion = im.item instanceof com.example.dungeon.model.Potion;
                drawFallback(canvas, sx, sy, isPotion ? 0xFFFF88FF : 0xFF44AAFF,
                             isPotion ? "p" : "i");
            }
        }

        // ── 4. Враги (только видимые) ─────────────────────────────────────
        for (Enemy enemy : engine.getEnemies()) {
            if (!map.getTile(enemy.getX(), enemy.getY()).isVisible()) continue;
            int sx = enemy.getX() - camX, sy = enemy.getY() - camY;
            if (!onScreen(sx, sy, visW, visH)) continue;

            boolean isBoss = enemy instanceof DragonBoss;
            int cells = isBoss ? 2 : 1;
            Bitmap bmp = tex.loadTransparent(enemy.getTexturePath());

            Paint paint = bmpOver;
            if (enemy.isDoorConfused() && (System.currentTimeMillis() / 300) % 2 == 0) {
                bmpOver.setAlpha(100);
            }
            if (bmp != null) drawBitmapPaint(canvas, bmp, sx, sy, cells, cells, paint);
            else drawFallback(canvas, sx, sy, isBoss ? 0xFFFF00FF : 0xFFFF4444, enemy.getSymbol());
            bmpOver.setAlpha(255);

            drawHpBar(canvas, sx, sy, enemy.getHp(), enemy.getMaxHp());
        }

        // ── 5. Игрок ──────────────────────────────────────────────────────
        int psx = player.getX() - camX, psy = player.getY() - camY;
        Bitmap playerBmp = tex.loadTransparent("mobs/player.png");
        if (playerBmp != null) drawBitmapPaint(canvas, playerBmp, psx, psy, 1, 1, bmpOver);
        else drawFallback(canvas, psx, psy, 0xFFFFD700, "@");
    }

    // ── Отрисовка базового тайла (стена / пол / дверь) ───────────────────

    private void drawBaseTile(Canvas canvas, GameMap map, Tile tile, int sx, int sy,
                              int variant, boolean visible, boolean doorOpen) {
        fillCell(canvas, sx, sy, Color.BLACK);

        String path = resolveBasePath(map, tile, variant, doorOpen);
        if (path == null) {
            if (visible) {
                fillCell(canvas, sx, sy, tileBg(tile));
                txtPaint.setColor(tileTextColor(tile));
                canvas.drawText(tile.getSymbol(), sx*CELL+CELL/2f, sy*CELL+CELL*0.76f, txtPaint);
            }
            return;
        }

        Bitmap bmp = tex.load(path);
        if (bmp == null) {
            if (visible) {
                fillCell(canvas, sx, sy, tileBg(tile));
                txtPaint.setColor(tileTextColor(tile));
                canvas.drawText(tile.getSymbol(), sx*CELL+CELL/2f, sy*CELL+CELL*0.76f, txtPaint);
            }
            return;
        }

        drawBitmapPaint(canvas, bmp, sx, sy, 1, 1, visible ? bmpFull : bmpDim);
    }

    private String resolveBasePath(GameMap map, Tile tile, int variant, boolean doorOpen) {
        switch (tile.getType()) {
            case WALL:  return map.getWallTexture();
            case FLOOR: return map.getFloorTexture();
            case DOOR:
                return doorOpen ? "tiles/open_door.png" : "tiles/door.png";
            case CHEST:
            case STAIRS_DOWN:
            case TRAP:
                return map.getFloorTexture();
            default: return null;
        }
    }

    private String getOverlayPath(Tile tile) {
        switch (tile.getType()) {
            case STAIRS_DOWN: return "tiles/stairs.png";
            case CHEST:       return "tiles/chest.png";
            case TRAP:
                return tile.isTrapRevealed() ? "tiles/trap_revealed.png" : null;
            default: return null;
        }
    }

    // ── Open doors ────────────────────────────────────────────────────────

    private Set<Integer> buildOpenDoors(GameMap map, Player player) {
        Set<Integer> set = new HashSet<>();
        int W = GameMap.WIDTH;
        if (map.getTile(player.getX(), player.getY()).getType() == Tile.Type.DOOR)
            set.add(player.getY() * W + player.getX());
        for (Enemy e : engine.getEnemies())
            if (map.getTile(e.getX(), e.getY()).getType() == Tile.Type.DOOR)
                set.add(e.getY() * W + e.getX());
        return set;
    }

    // ── HP-бар ────────────────────────────────────────────────────────────

    private void drawHpBar(Canvas canvas, int sx, int sy, int hp, int maxHp) {
        if (hp >= maxHp) return;
        float x = sx*CELL+2, y = sy*CELL-5, w = CELL-4, h = 4;
        fillPaint.setColor(0xFF440000);
        canvas.drawRect(x, y, x+w, y+h, fillPaint);
        fillPaint.setColor(0xFF00BB00);
        canvas.drawRect(x, y, x+w*(float)hp/maxHp, y+h, fillPaint);
    }

    // ── Примитивы ─────────────────────────────────────────────────────────

    private void drawFallback(Canvas canvas, int sx, int sy, int color, String sym) {
        fillPaint.setColor(color & 0x44FFFFFF);
        canvas.drawRect(sx*CELL+2, sy*CELL+2, (sx+1)*CELL-2, (sy+1)*CELL-2, fillPaint);
        txtPaint.setColor(color);
        canvas.drawText(sym, sx*CELL+CELL/2f, sy*CELL+CELL*0.76f, txtPaint);
    }

    private void fillCell(Canvas canvas, int sx, int sy, int color) {
        fillPaint.setColor(color);
        canvas.drawRect(sx*CELL, sy*CELL, (sx+1)*CELL, (sy+1)*CELL, fillPaint);
    }

    private void drawBitmapPaint(Canvas canvas, Bitmap bmp, int sx, int sy,
                                  int wC, int hC, Paint paint) {
        dst.set(sx*CELL, sy*CELL, (sx+wC)*CELL, (sy+hC)*CELL);
        canvas.drawBitmap(bmp, null, dst, paint);
    }

    // ── Заглушечные цвета ─────────────────────────────────────────────────

    private int tileBg(Tile tile) {
        switch (tile.getType()) {
            case WALL:        return 0xFF111122;
            case FLOOR:       return 0xFF0e0e1e;
            case DOOR:        return 0xFF0e0a05;
            case STAIRS_DOWN: return 0xFF001a11;
            case CHEST:       return 0xFF0e0c05;
            case TRAP:        return 0xFF0e0500;
            default:          return Color.BLACK;
        }
    }

    private int tileTextColor(Tile tile) {
        switch (tile.getType()) {
            case WALL:        return 0xFF444455;
            case FLOOR:       return 0xFF282838;
            case DOOR:        return 0xFFAA7733;
            case STAIRS_DOWN: return 0xFF00FF88;
            case CHEST:       return 0xFFFFD700;
            case TRAP:        return tile.isTrapRevealed() ? 0xFFFF8800 : 0xFF282838;
            default:          return 0xFF666666;
        }
    }

    private boolean onScreen(int sx, int sy, int vW, int vH) {
        return sx >= 0 && sx < vW && sy >= 0 && sy < vH;
    }
    private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
