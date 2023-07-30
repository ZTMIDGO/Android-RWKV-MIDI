package com.litesnap.open.rwkv.midi;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ZTMIDGO 2023/7/25
 */
public class ModelTockenizer {
    private final String FILE_NAME = "tokenizer-midi.json";
    private final Map<String, Integer> encoder = new HashMap<>();
    private final Map<Integer, String> decoder = new HashMap<>();

    public ModelTockenizer(Context context) throws FileNotFoundException {
        fillEncoder(context);
        fillDecoder();
    }

    public List<Integer> encode(String text) {
        return new ArrayList<>(Arrays.asList(0));
    }

    public List<String> decode(List<Integer> tokens) {
        List<String> result = new ArrayList<>();
        for (int token : tokens){
            if (decoder.containsKey(token)) result.add(decoder.get(token));
        }
        return result;
    }

    private void fillEncoder(Context context) throws FileNotFoundException {
        Gson gson = new Gson();
        String path = PathManager.getModelPath(context) + "/" + FILE_NAME;
        Type type = new TypeToken<Map<String, Integer>>() {}.getType();
        Inner inner = gson.fromJson(new FileReader(path), Inner.class);
        encoder.putAll(inner.model.vocab);
    }

    private void fillDecoder(){
        for (Map.Entry<String, Integer> entry : encoder.entrySet()) decoder.put(entry.getValue(), entry.getKey());
    }

    private class Inner{
        public M model;
        public class M{
            public Map<String, Integer> vocab;
        }
    }
}
