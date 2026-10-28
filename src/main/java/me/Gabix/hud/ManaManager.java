package me.Gabix.hud;

public class ManaManager {
    private int currentMana = 100;
    private int maxMana = 100;
    private int currentDamage = 0;

    public int getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(int mana) {
        this.currentMana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public int getCurrentDamage() {
        return currentDamage;
    }

    public void setCurrentDamage(int damage) {
        this.currentDamage = damage;
    }
}