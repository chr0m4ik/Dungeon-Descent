package com.example.dungeon.model;

import java.util.Random;

public class Potion extends Item {

    public enum Effect {
        HEAL, STRONG_HEAL, POISON_SELF, FIRE_SELF, BOOST_ATTACK, REVEAL_TRAPS
    }

    private static final String[] FAKE_NAMES = {
        "Мутное зелье", "Красное зелье", "Синее зелье",
        "Зелёное зелье", "Жёлтое зелье", "Пузырящееся зелье"
    };

    private static final String[] POTION_TEXTURES = {
        "items/potion_murky.png",
        "items/potion_red.png",
        "items/potion_blue.png",
        "items/potion_green.png",
        "items/potion_yellow.png",
        "items/potion_bubbling.png"
    };

    private final int potionId;

    public Potion(int potionId) {
        super(FAKE_NAMES[potionId], "🧪", Type.POTION, 0, 0,
              "Содержимое неизвестно... Выпить на свой страх и риск.",
              POTION_TEXTURES[potionId]);
        this.potionId = potionId;
    }

    public int getPotionId() { return potionId; }

    public static Effect randomEffect(Random rng) {
        Effect[] effects = Effect.values();
        return effects[rng.nextInt(effects.length)];
    }

    public static String effectDescription(Effect e) {
        switch (e) {
            case HEAL:          return "Лечение (слабое) +20 HP";
            case STRONG_HEAL:   return "Лечение (сильное) +40 HP";
            case POISON_SELF:   return "Яд — отравляет на 4 хода";
            case FIRE_SELF:     return "Огонь — горение 3 хода";
            case BOOST_ATTACK:  return "Усиление атаки +3 на 5 ходов";
            case REVEAL_TRAPS:  return "Обнаружение ловушек";
            default:            return "???";
        }
    }

    @Override
    public String getActionLabel() { return "Выпить"; }
}
