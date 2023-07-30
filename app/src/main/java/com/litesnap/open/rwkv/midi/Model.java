package com.litesnap.open.rwkv.midi;

import java.util.List;

/**
 * Created by ZTMIDGO 2022/9/9
 */
public interface Model {
    void generate(List<Integer> arrays, int maxCount, Callback callback);
    int sample(List<Integer> indexes, List<Float> probs);
    void close();
    void cancel();
    void setTopK(int value);
    void setPenalty(float v1, float v2);
    void clean();
    boolean isRunning();
    interface Callback{
        void callback(int maxCount, int position, boolean isEnd, List<Integer> tokens);
    }
}
