package com.example.dungeon;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dungeon.engine.GameEngine;
import com.example.dungeon.model.Item;
import com.example.dungeon.model.Player;
import com.example.dungeon.model.Potion;
import com.example.dungeon.model.StatusEffect;
import com.example.dungeon.ui.GameView;
import com.example.dungeon.ui.TextureManager;
import com.example.dungeon.achievements.AchievementManager;
import com.example.dungeon.achievements.Achievement;

import java.util.List;

public class GameActivity extends AppCompatActivity implements GameEngine.GameListener {

    private GameEngine engine;
    private GameView gameView;
    private TextView tvHUD;
    private TextView tvLog;
    private ScrollView scrollLog;

    private static final int BG        = Color.parseColor("#0d0d1a");
    private static final int BG_PANEL  = Color.parseColor("#111122");
    private static final int GOLD      = Color.parseColor("#FFD700");
    private static final int TEXT_DIM  = Color.parseColor("#888899");
    private static final int TEXT_MAIN = Color.parseColor("#ccccdd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // ── HUD ───────────────────────────────────────────────────────────
        tvHUD = new TextView(this);
        tvHUD.setTextColor(TEXT_MAIN);
        tvHUD.setTextSize(11f);
        tvHUD.setPadding(10, 6, 10, 6);
        tvHUD.setBackgroundColor(BG_PANEL);
        tvHUD.setTypeface(Typeface.MONOSPACE);
        root.addView(tvHUD);

        // ── Игровой экран ─────────────────────────────────────────────────
        gameView = new GameView(this);
        gameView.setBackgroundColor(Color.BLACK);
        root.addView(gameView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 3.5f));

        // ── Лог ───────────────────────────────────────────────────────────
        scrollLog = new ScrollView(this);
        tvLog = new TextView(this);
        tvLog.setTextColor(TEXT_DIM);
        tvLog.setTextSize(10f);
        tvLog.setPadding(10, 4, 10, 4);
        tvLog.setBackgroundColor(Color.parseColor("#080810"));
        tvLog.setTypeface(Typeface.MONOSPACE);
        scrollLog.addView(tvLog);
        root.addView(scrollLog, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ── Панель управления ─────────────────────────────────────────────
        root.addView(buildControls());

        setContentView(root);

        engine = new GameEngine();
        engine.setListener(this);

        AchievementManager am = AchievementManager.get(this);
        am.resetSession();
        engine.setAchievementManager(am);
        am.setUnlockListener(achievement -> {
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this,
                    "🏆 Достижение: " + achievement.getTitle(),
                    android.widget.Toast.LENGTH_LONG).show();
            });
        });
        gameView.setEngine(engine);

        updateHUD();
        updateLog();
    }

    // ── D-Pad и кнопки ────────────────────────────────────────────────────

    private View buildControls() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(BG_PANEL);
        bar.setPadding(8, 8, 8, 8);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout dpad = buildDPad();
        bar.addView(dpad, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(mkBtn("🎒  ИНВЕНТАРЬ", GOLD, Color.BLACK, v -> showInventory()));
        actions.addView(mkBtn("⏳  ЖДАТЬ", Color.parseColor("#555566"),
                              TEXT_MAIN, v -> engine.playerMove(0, 0)));
        bar.addView(actions, new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        return bar;
    }

    private LinearLayout buildDPad() {
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setGravity(Gravity.CENTER);

        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER);
        row1.addView(mkArrow("", null));
        row1.addView(mkArrow("↑", v -> engine.playerMove(0, -1)));
        row1.addView(mkArrow("", null));
        outer.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER);
        row2.addView(mkArrow("←", v -> engine.playerMove(-1, 0)));
        row2.addView(mkArrow("↓", v -> engine.playerMove(0,  1)));
        row2.addView(mkArrow("→", v -> engine.playerMove(1,  0)));
        outer.addView(row2);

        return outer;
    }

    private Button mkArrow(String label, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(TEXT_MAIN);
        btn.setTextSize(20f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setBackgroundColor(label.isEmpty()
            ? Color.TRANSPARENT : Color.parseColor("#222233"));

        int size = 72;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(lp);
        btn.setPadding(0, 0, 0, 0);
        if (listener != null) btn.setOnClickListener(listener);
        else btn.setEnabled(false);
        return btn;
    }

    private Button mkBtn(String label, int bgColor, int textColor,
                         View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextColor(textColor);
        btn.setBackgroundColor(bgColor);
        btn.setTextSize(13f);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 4, 0, 4);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(listener);
        return btn;
    }

    // ── ИНВЕНТАРЬ ─────────────────────────────────────────────────────────

    private void showInventory() {
        Player player = engine.getPlayer();
        List<Item> inv = player.getInventory();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0f0f1e"));
        root.setPadding(0, 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("  🎒  ИНВЕНТАРЬ");
        title.setTextColor(GOLD);
        title.setTextSize(16f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(16, 16, 16, 12);
        title.setBackgroundColor(Color.parseColor("#06060f"));
        root.addView(title);

        Item weapon = player.getEquippedWeapon();
        Item armor  = player.getEquippedArmor();
        if (weapon != null || armor != null) {
            LinearLayout equip = new LinearLayout(this);
            equip.setOrientation(LinearLayout.HORIZONTAL);
            equip.setPadding(16, 8, 16, 8);
            equip.setBackgroundColor(Color.parseColor("#0a0a18"));
            if (weapon != null) equip.addView(mkEquipBadge("⚔ " + weapon.getName()));
            if (armor  != null) equip.addView(mkEquipBadge("🛡 " + armor.getName()));
            root.addView(equip);
        }

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#333355"));
        root.addView(divider, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));

        if (inv.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("\n  Инвентарь пуст\n");
            empty.setTextColor(TEXT_DIM);
            empty.setTextSize(14f);
            empty.setGravity(Gravity.CENTER);
            root.addView(empty);
        } else {
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(8, 8, 8, 8);

            Dialog[] dlgRef = new Dialog[1];

            for (Item item : inv) {
                View row = mkItemRow(item, () -> {
                    if (dlgRef[0] != null) dlgRef[0].dismiss();
                    showItemDetail(item);
                });
                list.addView(row);
            }
            scroll.addView(list);
            root.addView(scroll);
        }

        Button close = mkBtn("  Закрыть", Color.parseColor("#222233"),
                             TEXT_DIM, null);
        root.addView(close);

        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        dlg.setContentView(root);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.92),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dlg.show();

        final Dialog[] arr = {dlg};
        close.setOnClickListener(v -> arr[0].dismiss());
        int N = arr.length;
        if (N > 0) arr[0] = dlg;
    }

    private View mkItemRow(Item item, Runnable onClick) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(12, 10, 12, 10);
        row.setBackgroundColor(Color.parseColor("#13132a"));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 2, 0, 2);
        row.setLayoutParams(lp);

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        Bitmap bmp = item.getTexturePath() != null
            ? TextureManager.get(this).load(item.getTexturePath()) : null;
        if (bmp != null) {
            icon.setImageBitmap(bmp);
        } else {
            Bitmap stub = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(stub);
            Paint p = new Paint();
            p.setColor(item instanceof Potion
                ? Color.parseColor("#882288")
                : item.getType() == Item.Type.WEAPON
                    ? Color.parseColor("#224488")
                    : Color.parseColor("#224422"));
            c.drawRect(0, 0, 32, 32, p);
            p.setColor(Color.WHITE);
            p.setTextSize(18);
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(item.getSymbol(), 16, 22, p);
            icon.setImageBitmap(stub);
        }
        row.addView(icon);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(12, 0, 0, 0);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0,
            ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(infoLp);

        TextView tvName = new TextView(this);
        tvName.setText(item.getName());
        tvName.setTextColor(TEXT_MAIN);
        tvName.setTextSize(14f);
        info.addView(tvName);

        String stats = item.getStatsLine();
        if (!stats.isEmpty()) {
            TextView tvStats = new TextView(this);
            tvStats.setText(stats);
            tvStats.setTextColor(GOLD);
            tvStats.setTextSize(11f);
            info.addView(tvStats);
        }

        row.addView(info);

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(TEXT_DIM);
        arrow.setTextSize(20f);
        row.addView(arrow);

        row.setOnClickListener(v -> onClick.run());

        row.setClickable(true);
        return row;
    }

    private TextView mkEquipBadge(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(GOLD);
        tv.setTextSize(11f);
        tv.setPadding(8, 4, 16, 4);
        return tv;
    }

    // ── Детали предмета ────────────────────────────────────────────────────

    private void showItemDetail(Item item) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0f0f1e"));
        root.setPadding(20, 20, 20, 20);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, 16);

        ImageView icon = new ImageView(this);
        int sz = 56;
        icon.setLayoutParams(new LinearLayout.LayoutParams(sz, sz));
        Bitmap bmp = item.getTexturePath() != null
            ? TextureManager.get(this).load(item.getTexturePath()) : null;
        if (bmp != null) {
            icon.setImageBitmap(bmp);
        } else {
            icon.setBackgroundColor(Color.parseColor("#222244"));
        }
        header.addView(icon);

        LinearLayout nameBlock = new LinearLayout(this);
        nameBlock.setOrientation(LinearLayout.VERTICAL);
        nameBlock.setPadding(16, 0, 0, 0);

        TextView tvName = new TextView(this);
        tvName.setText(item.getName());
        tvName.setTextColor(GOLD);
        tvName.setTextSize(18f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        nameBlock.addView(tvName);

        if (!item.getStatsLine().isEmpty()) {
            TextView tvStats = new TextView(this);
            tvStats.setText(item.getStatsLine());
            tvStats.setTextColor(Color.parseColor("#88dd88"));
            tvStats.setTextSize(13f);
            nameBlock.addView(tvStats);
        }
        header.addView(nameBlock);
        root.addView(header);

        TextView tvDesc = new TextView(this);
        String desc = item.getDescription();
        if (item instanceof Potion) {
            Potion p = (Potion) item;
            Potion.Effect knownEffect = engine.getPotionRegistry().get(p.getPotionId());
            desc = "Содержимое неизвестно. Выпить на свой страх и риск.";
        }
        tvDesc.setText(desc);
        tvDesc.setTextColor(TEXT_DIM);
        tvDesc.setTextSize(13f);
        tvDesc.setPadding(0, 0, 0, 20);
        root.addView(tvDesc);

        Dialog dlg = new Dialog(this, android.R.style.Theme_Black_NoTitleBar);
        dlg.setContentView(root);
        if (dlg.getWindow() != null)
            dlg.getWindow().setLayout(
                (int)(getResources().getDisplayMetrics().widthPixels * 0.88),
                ViewGroup.LayoutParams.WRAP_CONTENT);

        Player player = engine.getPlayer();
        boolean canUse = player.getInventory().contains(item);
        if (canUse) {
            Button btnUse = mkBtn("  " + item.getActionLabel(),
                GOLD, Color.BLACK, v -> {
                    dlg.dismiss();
                    engine.useItem(item);
                });
            root.addView(btnUse);
        }

        Button btnClose = mkBtn("  Закрыть",
            Color.parseColor("#222233"), TEXT_DIM, v -> dlg.dismiss());
        root.addView(btnClose);

        dlg.show();
    }

    // ── Обновление UI ─────────────────────────────────────────────────────

    private void updateHUD() {
        Player p = engine.getPlayer();
        StringBuilder sb = new StringBuilder();
        sb.append("❤ ").append(p.getHp()).append("/").append(p.getMaxHp())
          .append("  ⚔ ").append(p.getTotalAttack())
          .append("  🛡 ").append(p.getTotalDefense())
          .append("  💰 ").append(p.getGold())
          .append("  ☠ ").append(p.getKillCount())
          .append("  [Этаж ").append(engine.getCurrentFloor()).append("]");

        List<StatusEffect> effects = p.getStatusEffects();
        if (!effects.isEmpty()) {
            sb.append("  ");
            for (StatusEffect se : effects)
                sb.append(se.getIcon()).append(se.getTurnsLeft()).append(" ");
        }

        if (p.getEquippedWeapon() != null)
            sb.append("\n⚔ ").append(p.getEquippedWeapon().getName());
        if (p.getEquippedArmor() != null)
            sb.append("  🛡 ").append(p.getEquippedArmor().getName());

        tvHUD.setText(sb.toString());
    }

    private void updateLog() {
        StringBuilder sb = new StringBuilder();
        for (String line : engine.getLog()) sb.append(line).append("\n");
        tvLog.setText(sb.toString());
        scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
    }

    // ── GameListener ──────────────────────────────────────────────────────

    @Override
    public void onStateChanged() {
        runOnUiThread(() -> {
            updateHUD();
            updateLog();
            gameView.invalidate();
            checkEndState();
        });
    }

    @Override
    public void onLogMessage(String msg) {  }

    private void checkEndState() {
        GameEngine.GameState st = engine.getState();
        if (st == GameEngine.GameState.WIN) {
            showEndDialog("🏆 ПОБЕДА!",
                "Дракон повержен! Королевство спасено!\n\n💰 Золото: "
                + engine.getPlayer().getGold()
                + "\n☠ Убийства: " + engine.getPlayer().getKillCount());
        } else if (st == GameEngine.GameState.DEAD) {
            showEndDialog("💀 ПОРАЖЕНИЕ",
                "Тьма поглотила вас...\n\n💰 Золото: "
                + engine.getPlayer().getGold()
                + "\n☠ Убийства: " + engine.getPlayer().getKillCount()
                + "\n[Этаж " + engine.getCurrentFloor() + "]");
        }
    }

    private void showEndDialog(String title, String msg) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("Новая игра", (d, w) -> {
                TextureManager.reset();
                recreate();
            })
            .setNegativeButton("Выйти", (d, w) -> finish())
            .show();
    }
}
