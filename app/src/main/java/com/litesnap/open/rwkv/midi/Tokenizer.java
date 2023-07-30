package com.litesnap.open.rwkv.midi;

import java.util.List;

/**
 * Created by ZTMIDGO 2022/9/9
 */
public abstract interface Tokenizer {
    List<Integer> encode(String text);
    String decode(List<Integer> tokens);
}
