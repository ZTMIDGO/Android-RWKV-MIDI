package com.litesnap.open.rwkv.midi;

public abstract class MyRunnable implements Runnable {
    private boolean isCancel;

    public boolean isCancel() {
        return isCancel;
    }

    public void setCancel(boolean cancel) {
        isCancel = cancel;
    }
}
