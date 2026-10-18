package me.Gabix.hud;

public class ManaManager {
    private int currentMana = 100;
    private int currentDamage = 0;

    public int getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(int mana) {
        this.currentMana = mana;
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public void setCurrentDamage(int damage) {
        this.currentDamage = damage;
    }
}