package com.example.dungeon;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.dungeon.achievements.AchievementsActivity;
import com.example.dungeon.achievements.AchievementManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(40, 80, 40, 80);

        TextView title = new TextView(this);
        title.setText("⚔  DUNGEON DESCENT  ⚔");
        title.setTextColor(Color.parseColor("#FFD700"));
        title.setTextSize(24f);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("\nСредневековый рогалик\n");
        sub.setTextColor(Color.parseColor("#888888"));
        sub.setTextSize(13f);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        TextView desc = new TextView(this);
        desc.setText(
            "3 этажа подземелья + финальный босс\n\n" +
            "Враги: крысы → скелеты → тролли\n" +
            "Двери замедляют преследователей\n" +
            "Зелья с тайным эффектом\n" +
            "Яд, огонь, оглушение\n\n" +
            "Сохранений нет. Умер — начинай сначала."
        );
        desc.setTextColor(Color.parseColor("#AAAAAA"));
        desc.setTextSize(13f);
        desc.setGravity(Gravity.CENTER);
        root.addView(desc);

        Button btnStart = new Button(this);
        btnStart.setText("▶   НАЧАТЬ ИГРУ");
        btnStart.setTextColor(Color.BLACK);
        btnStart.setBackgroundColor(Color.parseColor("#FFD700"));
        btnStart.setTextSize(16f);
        btnStart.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 48;
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        btnStart.setLayoutParams(lp);
        btnStart.setPadding(60, 20, 60, 20);
        btnStart.setOnClickListener(v ->
            startActivity(new Intent(this, GameActivity.class)));
        root.addView(btnStart);

        Button btnAch = new Button(this);
        btnAch.setText("🏆   ДОСТИЖЕНИЯ");
        btnAch.setTextColor(Color.parseColor("#FFD700"));
        btnAch.setBackgroundColor(Color.parseColor("#1a1a2e"));
        btnAch.setTextSize(14f);
        btnAch.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams achLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        achLp.topMargin = 24;
        achLp.gravity = Gravity.CENTER_HORIZONTAL;
        btnAch.setLayoutParams(achLp);
        btnAch.setPadding(40, 16, 40, 16);
        btnAch.setOnClickListener(v ->
            startActivity(new Intent(this, AchievementsActivity.class)));
        root.addView(btnAch);

        AchievementManager.get(this).loadFromGitHub(null);

        TextView tips = new TextView(this);
        tips.setText(
            "\n\n── КАК ИГРАТЬ ──\n" +
            "Стрелки — движение / атака\n" +
            "Зайди за дверь — враг потеряет тебя\n" +
            "Вернись сразу — получишь свободный удар\n" +
            "Зелья неизвестны до первого глотка\n" +
            "Коснись предмета — подберёшь автоматически"
        );
        tips.setTextColor(Color.parseColor("#555566"));
        tips.setTextSize(11f);
        tips.setGravity(Gravity.CENTER);
        root.addView(tips);

        setContentView(root);
    }
}
