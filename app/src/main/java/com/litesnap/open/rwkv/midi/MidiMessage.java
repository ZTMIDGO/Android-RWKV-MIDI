package com.litesnap.open.rwkv.midi;

public class MidiMessage {
    public boolean is_meta = false;
    public String type;
    public int channel;
    public int note;
    public int velocity;
    public int time;
    public String text;
    public int tempo;
    public int program;

    public MidiMessage(){}

    public MidiMessage(String type, int note, int time, int channel) {
        this(type, note, 64, time, channel);
    }


    public MidiMessage(String type, int note, int velocity, int time, int channel) {
        this.type = type;
        this.note = note;
        this.velocity = velocity;
        this.time = time;
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "MidiMessage{" +
                "type='" + type + '\'' +
                ", channel=" + channel +
                ", note=" + note +
                ", velocity=" + velocity +
                ", time=" + time +
                ", program=" + program +
                '}'+"\n";
    }
}
