package com.example.dungeon.achievements;

public class Achievement {

    public enum Type {
        KILLS,
        BOSS,
        ITEMS,
        FLOOR,
        POTIONS,
        NODAMAGE
    }

    private final String id;
    private final String title;
    private final String desc;
    private final int    goal;
    private final Type   type;

    private int progress = 0;
    private boolean unlocked = false;

    public Achievement(String id, String title, String desc, int goal, Type type) {
        this.id    = id;
        this.title = title;
        this.desc  = desc;
        this.goal  = goal;
        this.type  = type;
    }

    public String  getId()       { return id; }
    public String  getTitle()    { return title; }
    public String  getDesc()     { return desc; }
    public int     getGoal()     { return goal; }
    public Type    getType()     { return type; }
    public int     getProgress() { return progress; }
    public boolean isUnlocked()  { return unlocked; }

    public void setProgress(int p) {
        this.progress = p;
        if (p >= goal) this.unlocked = true;
    }

    public void setUnlocked(boolean u) { this.unlocked = u; }

    public void increment() { setProgress(progress + 1); }

    public String getProgressText() {
        if (unlocked) return "✓ Выполнено";
        return progress + " / " + goal;
    }
}
