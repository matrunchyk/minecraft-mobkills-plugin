package com.matrunchyk.mobkillsplugin.models;

import org.bukkit.Note;

public class DelayedNote {
    private final Note note;
    private final NoteDelay delay;

    public DelayedNote(Note note, NoteDelay delay) {
        this.note = note;
        this.delay = delay;
    }

    public Note getNote() {return note;}

    public NoteDelay getDelay() {
        return delay;
    }
}
