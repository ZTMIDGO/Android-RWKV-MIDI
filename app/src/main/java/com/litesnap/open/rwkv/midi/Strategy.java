package com.litesnap.open.rwkv.midi;

/**
 * Created by ZTMIDGO 2022/9/9
 */
public class Strategy {
    public StrategyEnum strategy;
    public int value = 0;

    public Strategy(StrategyEnum strategy, int value) {
        this.strategy = strategy;
        this.value = value;
    }
}
