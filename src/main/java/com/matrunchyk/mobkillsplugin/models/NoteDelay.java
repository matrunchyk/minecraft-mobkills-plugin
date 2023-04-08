package com.matrunchyk.mobkillsplugin.models;

public enum NoteDelay {
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8);

    private final int ticks;

    NoteDelay(int ticks) {
        this.ticks = ticks;
    }

    public int getTicks() {
        return ticks;
    }
}

