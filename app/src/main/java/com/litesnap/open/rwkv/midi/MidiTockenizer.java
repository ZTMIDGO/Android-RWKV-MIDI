package com.litesnap.open.rwkv.midi;


import java.util.*;

public class MidiTockenizer {
    private VocabConfig config;

    public MidiTockenizer(VocabConfig config){
        this.config = config;
    }

    public Pair<List<MidiMessage>, DecodeState> str_to_midi_messages(String data, float end_token_pause){
        DecodeState state = new DecodeState(0, 0, config._short_instrument_names_str_to_int.get(config.short_instr_bin_names.get(0)), 0, new LinkedHashMap<>());
        String[] array = data.split(" ");
        List<MidiMessage> result = new ArrayList<>();

        outside:
        for (String token : array){
            token = token.trim();
            if (token.equals("<end>")){
                float d = end_token_pause * 1000f;
                state.delta_accum += d;
                state.total_time += d;
                if (config.decode_end_held_note_delay != 0){
                    Set<Pair<Integer, Integer>> removes = new HashSet<>();
                    for (Map.Entry<Pair<Integer, Integer>, Float> entry : state.active_notes.entrySet()){
                        int channel = entry.getKey().first;
                        int note = entry.getKey().second;
                        float start_time = entry.getValue();

                        int ticks = (int) Mido.second2tick(state.delta_accum / 1000.0, 480, 500000);
                        state.delta_accum = 0;
                        removes.add(entry.getKey());
                        result.add(new MidiMessage("note_off", note, ticks, channel));
                    }

                    for (Pair<Integer, Integer> pair : removes) state.active_notes.remove(pair);
                    continue outside;
                }
            }

            if (token.startsWith("<")){
                continue;
            }

            if (config.unrolled_tokens){

            }else {
                String[] tokens = StringUtils.toArrays(token);
                if (tokens[0].equals("t") && tokens[1].matches("[0-9]")){
                    float d = Mido.wait_token_to_delta(config, Integer.parseInt(token.substring(1, token.length())));
                    state.delta_accum += d;
                    state.total_time += d;
                    if (config.decode_end_held_note_delay != 0){
                        Set<Pair<Integer, Integer>> removes = new HashSet<>();
                        for (Map.Entry<Pair<Integer, Integer>, Float> entry : state.active_notes.entrySet()){
                            int channel = entry.getKey().first;
                            int note = entry.getKey().second;
                            float start_time = entry.getValue();

                            if (state.total_time - start_time > config.decode_end_held_note_delay * 1000f){
                                int ticks = (int) Mido.second2tick(state.delta_accum / 1000.0, 480, 500000);
                                state.delta_accum = 0.0f;
                                removes.add(entry.getKey());
                                result.add(new MidiMessage("note_off", note, ticks, channel));
                            }
                        }
                        for (Pair<Integer, Integer> pair : removes) state.active_notes.remove(pair);
                    }
                }else {
                    int[] notes = Mido.note_token_to_data(config, token);
                    int bin = notes[0];
                    int note = notes[1];
                    int velocity = notes[2];
                    int channel = config.bin_channel_map.get(config.bin_instrument_names.get(bin));
                    int ticks = (int) Mido.second2tick(state.delta_accum / 1000.0, 480, 500000);
                    state.delta_accum = 0.0f;
                    Pair<Integer, Integer> pair = new Pair<>(channel, note);

                    if (velocity > 0){
                        if (config.decode_fix_repeated_notes){
                            if (state.active_notes.containsKey(pair)){
                                state.active_notes.remove(pair);
                            }
                            result.add(new MidiMessage("note_off", note, ticks, channel));
                            ticks = 0;
                        }
                        state.active_notes.put(pair, state.total_time);
                        result.add(new MidiMessage("note_on", note, velocity, ticks, channel));
                    }else {
                        if (state.active_notes.containsKey(pair)){
                            state.active_notes.remove(pair);
                        }
                        result.add(new MidiMessage("note_off", note, ticks, channel));
                    }
                }
            }
        }
        return new Pair<>(result, state);
    }

    public List<MidiMessage> getProgramChange(){
        List<MidiMessage> list = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : config.bin_channel_map.entrySet()){
            MidiMessage msg = new MidiMessage();
            msg.type = "program_change";
            msg.channel = entry.getValue();
            msg.program = entry.getValue() == 9 ? 0 : config._instrument_names_str_to_int.get(config.bin_name_to_program_name.get(entry.getKey()));
            msg.time = 0;
            list.add(msg);
        }
        return list;
    }
}
