package com.example.dungeon.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {

    private static TextureManager instance;
    private final Map<String, Bitmap> cache = new HashMap<>();
    private final Context ctx;

    private TextureManager(Context context) {
        this.ctx = context.getApplicationContext();
    }

    public static TextureManager get(Context context) {
        if (instance == null) instance = new TextureManager(context);
        return instance;
    }

    public static void reset() {
        if (instance != null) {
            for (Bitmap b : instance.cache.values())
                if (b != null && !b.isRecycled()) b.recycle();
            instance.cache.clear();
            instance = null;
        }
    }

    public Bitmap load(String path) {
        if (cache.containsKey(path)) return cache.get(path);
        try {
            InputStream is = ctx.getAssets().open("textures/" + path);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap src = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            if (src == null) { cache.put(path, null); return null; }
            Bitmap result = flattenOnBlack(src);
            if (result != src) src.recycle();
            cache.put(path, result);
            return result;
        } catch (IOException e) {
            cache.put(path, null);
            return null;
        }
    }

    public Bitmap loadTransparent(String path) {
        String key = path + "#transparent";
        if (cache.containsKey(key)) return cache.get(key);
        try {
            InputStream is = ctx.getAssets().open("textures/" + path);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap src = BitmapFactory.decodeStream(is, null, opts);
            is.close();
            if (src == null) { cache.put(key, null); return null; }
            Bitmap result = makeBlackTransparent(src);
            src.recycle();
            cache.put(key, result);
            return result;
        } catch (IOException e) {
            cache.put(key, null);
            return null;
        }
    }

    // ── Обработка bitmap ──────────────────────────────────────────────────

    private static Bitmap flattenOnBlack(Bitmap src) {
        Bitmap out = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(0xFF000000);
        c.drawBitmap(src, 0, 0, null);
        return out;
    }

    private static Bitmap makeBlackTransparent(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int px = pixels[i];
            int r = Color.red(px), g = Color.green(px), b = Color.blue(px);
            if (r + g + b < 30) {
                pixels[i] = 0x00000000;
            }
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }
}
