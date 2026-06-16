package com.example.dungeon.achievements;

public class AchievementConfig {
    public static final String GITHUB_USER = "chr0m4ik";
    public static final String GITHUB_REPO = "Dungeon-Descent";
    public static final String BRANCH = "main";
    public static final String FILE = "achievements.json";

    public static String getUrl() {
        return "https://raw.githubusercontent.com/"
             + GITHUB_USER + "/" + GITHUB_REPO + "/"
             + BRANCH + "/" + FILE;
    }
}
