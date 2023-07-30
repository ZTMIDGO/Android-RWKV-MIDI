package com.litesnap.open.rwkv.midi;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class VocabConfig {
    public int note_events;
    public int wait_events;
    public int max_wait_time;
    public int velocity_events;
    public int velocity_bins;
    public float velocity_exp;
    public boolean do_token_sorting;
    public boolean unrolled_tokens;
    public float decode_end_held_note_delay;
    public boolean decode_fix_repeated_notes;
    public String ch10_instrument_bin_name;
    public int _ch10_bin_int;

    public final List<String> bin_instrument_names = new ArrayList<>();
    public final Map<String, String> program_name_to_bin_name = new LinkedHashMap<>();
    public final Map<String, String> bin_name_to_program_name = new LinkedHashMap<>();
    public final Map<String, String> instrument_names = new LinkedHashMap<>();

    public final Map<String, Integer> _instrument_names_str_to_int = new HashMap<>();
    public final Map<Integer, String> _instrument_names_int_to_str = new HashMap<>();
    public final Map<String, Integer> _bin_str_to_int = new HashMap<>();
    public final List<Integer> _bin_int_to_instrument_int = new ArrayList<>();
    public final List<Integer> _instrument_int_to_bin_int = new ArrayList<>();
    public final List<String> short_instr_bin_names = new ArrayList<>();
    public final Map<String, Integer> _short_instrument_names_str_to_int = new HashMap<>();
    public final Map<String, Integer> bin_channel_map = new LinkedHashMap<>();

    public static VocabConfig fromJson(String path) throws FileNotFoundException {
        Gson gson = new Gson();
        VocabConfig config = new VocabConfig();
        JsonObject jsonObject = gson.fromJson(new JsonReader(new FileReader(path)), JsonObject.class);

        JsonElement element = jsonObject.get("note_events");
        config.note_events = element.getAsInt();

        element = jsonObject.get("wait_events");
        config.wait_events = element.getAsInt();

        element = jsonObject.get("max_wait_time");
        config.max_wait_time = element.getAsInt();

        element = jsonObject.get("velocity_events");
        config.velocity_events = element.getAsInt();

        element = jsonObject.get("velocity_bins");
        config.velocity_bins = element.getAsInt();

        element = jsonObject.get("velocity_exp");
        config.velocity_exp = element.getAsFloat();

        element = jsonObject.get("do_token_sorting");
        config.do_token_sorting = element.getAsBoolean();

        element = jsonObject.get("unrolled_tokens");
        config.unrolled_tokens = element.getAsBoolean();

        element = jsonObject.get("decode_end_held_note_delay");
        config.decode_end_held_note_delay = element.getAsFloat();

        element = jsonObject.get("decode_fix_repeated_notes");
        config.decode_fix_repeated_notes = element.getAsBoolean();

        element = jsonObject.get("ch10_instrument_bin_name");
        config.ch10_instrument_bin_name = element.getAsString();

        element = jsonObject.get("bin_instrument_names");
        config.bin_instrument_names.addAll(Arrays.asList(gson.fromJson(element, String[].class)));

        element = jsonObject.get("program_name_to_bin_name");
        Set<Map.Entry<String, JsonElement>> set = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : set){
            config.program_name_to_bin_name.put(entry.getKey(), entry.getValue().getAsString());
        }

        element = jsonObject.get("bin_name_to_program_name");
        set = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : set){
            config.bin_name_to_program_name.put(entry.getKey(), entry.getValue().getAsString());
        }

        element = jsonObject.get("instrument_names");
        set = element.getAsJsonObject().entrySet();
        for (Map.Entry<String, JsonElement> entry : set){
            config.instrument_names.put(entry.getKey(), entry.getValue().getAsString());
        }
        return config;
    }

    public void init(){
        for (Map.Entry<String, String> entry : instrument_names.entrySet()){
            _instrument_names_str_to_int.put(entry.getValue(), Integer.parseInt(entry.getKey()));
            _instrument_names_int_to_str.put(Integer.parseInt(entry.getKey()), entry.getValue());
        }

        for (int i = 0; i < bin_instrument_names.size(); i++){
            _bin_str_to_int.put(bin_instrument_names.get(i), i);
        }

        for (int i = 0; i < bin_instrument_names.size(); i++){
            String name = bin_instrument_names.get(i);
            _bin_int_to_instrument_int.add(!name.equals(ch10_instrument_bin_name) ? _instrument_names_str_to_int.get(bin_name_to_program_name.get(name)) : 0);
        }

        for (Map.Entry<String, String> entry : program_name_to_bin_name.entrySet()){
            String instr = entry.getKey();
            _instrument_int_to_bin_int.add(!TextUtils.isEmpty(program_name_to_bin_name.get(instr)) ? _bin_str_to_int.get(program_name_to_bin_name.get(instr)) : -1);
        }

        _ch10_bin_int = !TextUtils.isEmpty(ch10_instrument_bin_name) ? _bin_str_to_int.get(ch10_instrument_bin_name) : -1;

        for (String instr : bin_instrument_names){
            int i = Math.min(1, instr.length());
            while (i <= instr.length() && short_instr_bin_names.contains(instr.substring(0, i))){
                i += 1;
            }
            short_instr_bin_names.add(instr.substring(0, i));
        }

        for (int i = 0; i < short_instr_bin_names.size(); i++){
            String name = short_instr_bin_names.get(i);
            _short_instrument_names_str_to_int.put(name, i);
        }

        List<Integer> range_excluding_ch10 = new ArrayList<>();
        for (int i =0; i < bin_instrument_names.size(); i++){
            range_excluding_ch10.add(i < 9 ? i : i + 1);
        }

        List<String> bins_excluding_ch10 = new ArrayList<>();
        for (String n : bin_instrument_names) {
            if (!n.equals(ch10_instrument_bin_name)) bins_excluding_ch10.add(n);
        }

        int size = range_excluding_ch10.size() > bins_excluding_ch10.size() ? bins_excluding_ch10.size() : range_excluding_ch10.size();
        for (int i =0; i < size; i++){
            int channel = range_excluding_ch10.get(i);
            String bin = bins_excluding_ch10.get(i);
            bin_channel_map.put(bin, channel);
        }

        if (!TextUtils.isEmpty(ch10_instrument_bin_name)) bin_channel_map.put(ch10_instrument_bin_name, 9);
    }
}
