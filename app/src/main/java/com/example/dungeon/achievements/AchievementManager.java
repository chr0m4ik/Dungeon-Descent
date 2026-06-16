package com.example.dungeon.achievements;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AchievementManager {

    private static AchievementManager instance;

    private final List<Achievement> achievements = new ArrayList<>();
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler   = new Handler(Looper.getMainLooper());

    private int sessionKills   = 0;
    private int sessionItems   = 0;
    private int sessionPotions = 0;
    private int maxFloorReached = 1;
    private boolean tookDamageThisFloor = false;

    public interface OnUnlockListener {
        void onUnlock(Achievement a);
    }
    private OnUnlockListener unlockListener;

    // ── Singleton ─────────────────────────────────────────────────────────

    public static AchievementManager get(Context context) {
        if (instance == null) instance = new AchievementManager(context);
        return instance;
    }

    private AchievementManager(Context context) {
        prefs = context.getSharedPreferences("achievements", Context.MODE_PRIVATE);
    }

    public void setUnlockListener(OnUnlockListener l) { this.unlockListener = l; }

    // ── Загрузка достижений с GitHub ──────────────────────────────────────

    public void loadFromGitHub(Runnable onReady) {
        executor.execute(() -> {
            try {
                String json = fetchUrl(AchievementConfig.getUrl());
                parseJson(json);
            } catch (Exception e) {
                loadDefaults();
            }
            restoreProgress();
            mainHandler.post(() -> { if (onReady != null) onReady.run(); });
        });
    }

    // ── Парсинг JSON ──────────────────────────────────────────────────────

    private void parseJson(String json) throws Exception {
        achievements.clear();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            String id    = obj.getString("id");
            String title = obj.getString("title");
            String desc  = obj.getString("desc");
            int    goal  = obj.getInt("goal");
            String type  = obj.getString("type").toUpperCase();
            Achievement.Type aType = Achievement.Type.valueOf(type);
            achievements.add(new Achievement(id, title, desc, goal, aType));
        }
    }

    private void loadDefaults() {
        achievements.clear();
        achievements.add(new Achievement("kill_5",    "Охотник",        "Убей 5 врагов за сессию",       5,  Achievement.Type.KILLS));
        achievements.add(new Achievement("kill_20",   "Истребитель",    "Убей 20 врагов за сессию",      20, Achievement.Type.KILLS));
        achievements.add(new Achievement("kill_boss", "Убийца дракона", "Победи финального босса",       1,  Achievement.Type.BOSS));
        achievements.add(new Achievement("loot_5",    "Собиратель",     "Подбери 5 предметов",           5,  Achievement.Type.ITEMS));
        achievements.add(new Achievement("loot_10",   "Мародёр",        "Подбери 10 предметов",          10, Achievement.Type.ITEMS));
        achievements.add(new Achievement("floor_3",   "Смельчак",       "Дойди до 3-го этажа",           3,  Achievement.Type.FLOOR));
        achievements.add(new Achievement("floor_4",   "Герой",          "Дойди до логова дракона",       4,  Achievement.Type.FLOOR));
        achievements.add(new Achievement("potions_3", "Алхимик",        "Выпей 3 зелья за сессию",       3,  Achievement.Type.POTIONS));
        achievements.add(new Achievement("nodamage",  "Призрак",        "Пройди этаж без урона",         1,  Achievement.Type.NODAMAGE));
    }

    // ── Прогресс ──────────────────────────────────────────────────────────

    private void restoreProgress() {
        for (Achievement a : achievements) {
            boolean unlocked = prefs.getBoolean("unlocked_" + a.getId(), false);
            a.setUnlocked(unlocked);
            if (unlocked) a.setProgress(a.getGoal());
        }
    }

    private void saveUnlocked(Achievement a) {
        prefs.edit().putBoolean("unlocked_" + a.getId(), true).apply();
    }

    // ── Трекинг событий из игры ───────────────────────────────────────────

    public void onEnemyKilled() {
        sessionKills++;
        checkAll();
    }

    public void onBossKilled() {
        sessionKills++;
        for (Achievement a : achievements)
            if (a.getType() == Achievement.Type.BOSS && !a.isUnlocked()) {
                a.setProgress(a.getGoal());
                notifyUnlock(a);
                saveUnlocked(a);
            }
        checkAll();
    }

    public void onItemPickedUp() {
        sessionItems++;
        checkAll();
    }

    public void onPotionUsed() {
        sessionPotions++;
        checkAll();
    }

    public void onFloorReached(int floor) {
        if (floor > maxFloorReached) maxFloorReached = floor;
        tookDamageThisFloor = false;
        checkAll();
    }

    public void onPlayerTookDamage() {
        tookDamageThisFloor = true;
    }

    public void onFloorCleared() {
        if (!tookDamageThisFloor) {
            for (Achievement a : achievements)
                if (a.getType() == Achievement.Type.NODAMAGE && !a.isUnlocked()) {
                    a.setProgress(a.getGoal());
                    notifyUnlock(a);
                    saveUnlocked(a);
                }
        }
    }

    // ── Проверка всех достижений ──────────────────────────────────────────

    private void checkAll() {
        for (Achievement a : achievements) {
            if (a.isUnlocked()) continue;
            int progress = getCurrentProgress(a);
            a.setProgress(progress);
            if (a.isUnlocked()) {
                notifyUnlock(a);
                saveUnlocked(a);
            }
        }
    }

    private int getCurrentProgress(Achievement a) {
        switch (a.getType()) {
            case KILLS:   return sessionKills;
            case ITEMS:   return sessionItems;
            case POTIONS: return sessionPotions;
            case FLOOR:   return maxFloorReached;
            default:      return 0;
        }
    }

    private void notifyUnlock(Achievement a) {
        mainHandler.post(() -> {
            if (unlockListener != null) unlockListener.onUnlock(a);
        });
    }

    // ── HTTP запрос ───────────────────────────────────────────────────────

    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();

        if (conn.getResponseCode() != 200)
            throw new Exception("HTTP " + conn.getResponseCode());

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    // ── Геттеры ───────────────────────────────────────────────────────────

    public List<Achievement> getAchievements() { return achievements; }

    public void resetSession() {
        sessionKills   = 0;
        sessionItems   = 0;
        sessionPotions = 0;
        maxFloorReached = 1;
        tookDamageThisFloor = false;
    }
}
