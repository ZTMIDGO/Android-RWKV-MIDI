package com.litesnap.open.rwkv.midi;

import java.util.Map;

public class DecodeState {
    public float total_time;
    public float delta_accum;
    public int current_bin;
    public int current_note;
    public Map<Pair<Integer, Integer>, Float> active_notes;

    public DecodeState(float total_time, float delta_accum, int current_bin, int current_note, Map<Pair<Integer, Integer>, Float> active_notes) {
        this.total_time = total_time;
        this.delta_accum = delta_accum;
        this.current_bin = current_bin;
        this.current_note = current_note;
        this.active_notes = active_notes;
    }
}
