package com.example.dungeon.achievements;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dungeon.achievements.Achievement;
import com.example.dungeon.achievements.AchievementManager;

import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private static final int BG       = 0xFF0a0a14;
    private static final int BG_CARD  = 0xFF13132a;
    private static final int GOLD     = 0xFFFFD700;
    private static final int GREEN    = 0xFF44CC44;
    private static final int DIM      = 0xFF666677;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        TextView title = new TextView(this);
        title.setText("🏆  ДОСТИЖЕНИЯ");
        title.setTextColor(GOLD);
        title.setTextSize(18f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(20, 20, 20, 16);
        title.setBackgroundColor(0xFF060610);
        root.addView(title);

        TextView tvStatus = new TextView(this);
        tvStatus.setPadding(20, 8, 20, 8);
        tvStatus.setTextColor(DIM);
        tvStatus.setTextSize(11f);
        root.addView(tvStatus);

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(12, 8, 12, 20);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        AchievementManager mgr = AchievementManager.get(this);
        tvStatus.setText("⏳ Загрузка достижений с GitHub...");

        mgr.loadFromGitHub(() -> {
            tvStatus.setText("✓ Загружено с: " + AchievementConfig.getUrl());
            list.removeAllViews();
            buildAchievementList(list, mgr.getAchievements());
        });
    }

    private void buildAchievementList(LinearLayout list, List<Achievement> achievements) {
        int unlocked = 0;
        for (Achievement a : achievements) if (a.isUnlocked()) unlocked++;

        TextView counter = new TextView(this);
        counter.setText("Выполнено: " + unlocked + " / " + achievements.size());
        counter.setTextColor(GOLD);
        counter.setTextSize(13f);
        counter.setPadding(8, 4, 8, 16);
        list.addView(counter);

        for (Achievement a : achievements) {
            list.addView(buildCard(a));
        }
    }

    private LinearLayout buildCard(Achievement a) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(a.isUnlocked() ? 0xFF1a2a1a : BG_CARD);
        card.setPadding(16, 14, 16, 14);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 6);
        card.setLayoutParams(lp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView icon = new TextView(this);
        icon.setText(a.isUnlocked() ? "🏆" : "🔒");
        icon.setTextSize(20f);
        icon.setPadding(0, 0, 12, 0);
        header.addView(icon);

        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameBlock.setLayoutParams(nbLp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(a.getTitle());
        tvTitle.setTextColor(a.isUnlocked() ? GREEN : 0xFFCCCCDD);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        nameBlock.addView(tvTitle);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(a.getDesc());
        tvDesc.setTextColor(DIM);
        tvDesc.setTextSize(11f);
        nameBlock.addView(tvDesc);

        header.addView(nameBlock);

        TextView tvProgress = new TextView(this);
        tvProgress.setText(a.getProgressText());
        tvProgress.setTextColor(a.isUnlocked() ? GREEN : GOLD);
        tvProgress.setTextSize(12f);
        tvProgress.setGravity(Gravity.END);
        header.addView(tvProgress);

        card.addView(header);

        if (!a.isUnlocked() && a.getGoal() > 1) {
            ProgressBar bar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
            bar.setMax(a.getGoal());
            bar.setProgress(a.getProgress());
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 8);
            barLp.setMargins(0, 8, 0, 0);
            bar.setLayoutParams(barLp);
            card.addView(bar);
        }

        return card;
    }
}
